package jp.citybuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * クライアントから分割送信された .schem を組み立て、検証して templates/ に保存する。
 * 1パケットの上限(約32KB)を避けるため、クライアントは 30000 バイトずつ送る。
 */
public final class UploadManager {
    private static final class Upload {
        boolean plan;
        String id, name, category, desc;
        byte[] data;
        int pos;
    }

    private static final Map<UUID, Upload> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_BEGIN = new HashMap<>();

    private UploadManager() {}

    public static void clear() { ACTIVE.clear(); LAST_BEGIN.clear(); }

    private static String clean(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("[\\p{Cntrl}]", "").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String planKey(ServerPlayerEntity p, String id) {
        String who = p.getGameProfile().getName().replaceAll("[^A-Za-z0-9_]", "_");
        return who + "_" + id;
    }

    public static void begin(ServerPlayerEntity p, String kind, String id, String name, String category, String desc, int total) {
        ACTIVE.remove(p.getUuid());
        CityConfig c = CityBuilderMod.config();
        boolean plan = "plan".equals(kind);
        if (!plan && !"schem".equals(kind)) { CityActions.reply(p, "不明なアップロード種別です", false); return; }
        if (!PlacementValidator.canUpload(p)) { CityActions.reply(p, "アップロードの権限がありません", false); return; }
        int now = p.getServer().getTicks();
        Integer last = LAST_BEGIN.get(p.getUuid());
        if (last != null && now - last < c.uploadCooldownTicks) { CityActions.reply(p, "アップロードの間隔が短すぎます", false); return; }
        LAST_BEGIN.put(p.getUuid(), now);
        if (!BuildingCatalog.validId(id) || (plan && id.length() > 40)) {
            CityActions.reply(p, "IDは半角英数字と _ - の" + (plan ? 40 : 48) + "文字以内にしてください", false);
            return;
        }
        if (total <= 0 || total > c.maxUploadBytes) {
            CityActions.reply(p, "ファイルが大きすぎます(最大 " + c.maxUploadBytes / 1000 + " KB)", false);
            return;
        }
        if (!plan) {
            if (CityBuilderMod.catalog().has(id) && !c.allowUploadOverwrite) {
                CityActions.reply(p, "同じIDの建物が既にあります: " + id, false);
                return;
            }
            if (!CityBuilderMod.catalog().has(id) && CityBuilderMod.catalog().uploadedCount() >= c.maxUploadedTemplates) {
                CityActions.reply(p, "アップロードできる建物の数が上限です(管理者に削除を依頼してください)", false);
                return;
            }
        } else if (PlanLoader.list().size() >= 1000) {
            CityActions.reply(p, "保存できるプランの数が上限です", false);
            return;
        }
        Upload u = new Upload();
        u.plan = plan;
        u.id = id;
        u.name = clean(name, 64);
        if (u.name.isEmpty()) u.name = id;
        u.category = clean(category, 32);
        if (u.category.isEmpty()) u.category = "アップロード";
        u.desc = clean(desc, 200);
        u.data = new byte[total];
        ACTIVE.put(p.getUuid(), u);
    }

    public static void chunk(ServerPlayerEntity p, byte[] part) {
        Upload u = ACTIVE.get(p.getUuid());
        if (u == null) return;
        if (part.length == 0 || u.pos + part.length > u.data.length) {
            ACTIVE.remove(p.getUuid());
            CityActions.reply(p, "アップロードデータが不正です", false);
            return;
        }
        System.arraycopy(part, 0, u.data, u.pos, part.length);
        u.pos += part.length;
        if (u.pos < u.data.length) return;
        ACTIVE.remove(p.getUuid());
        if (u.plan) finishPlan(p, u);
        else finish(p, u);
    }

    private static void finish(ServerPlayerEntity p, Upload u) {
        if (!PlacementValidator.canUpload(p)) { CityActions.reply(p, "アップロードの権限がありません", false); return; }
        CityConfig c = CityBuilderMod.config();
        Template t;
        try {
            t = Template.load(u.id, u.data);
        } catch (IOException | RuntimeException e) {
            CityActions.reply(p, "schemを読めませんでした: " + e.getMessage(), false);
            return;
        }
        if (t.volume() > c.maxVolume) { CityActions.reply(p, "建物が大きすぎます", false); return; }
        BuildingCatalog.Entry e = new BuildingCatalog.Entry();
        e.id = u.id; e.name = u.name; e.category = u.category; e.description = u.desc;
        e.floors = Math.max(1, t.h / 4);
        e.author = p.getGameProfile().getName();
        try {
            CityBuilderMod.catalog().addUpload(e, t, Arrays.copyOf(u.data, u.data.length));
        } catch (IOException ex) {
            CityActions.reply(p, "保存に失敗しました: " + ex.getMessage(), false);
            return;
        }
        CityBuilderMod.LOG.info("{} が建物 {} をアップロードしました ({} bytes)", p.getGameProfile().getName(), u.id, u.data.length);
        CityActions.reply(p, "アップロード完了: " + u.name + " (" + t.w + "x" + t.h + "x" + t.l + ")", true);
        ServerNet.broadcastCatalog(p.getServer());
    }

    /** WebUIが書き出した city.json を保存し、足元を原点としてプレビューを出す。 */
    private static void finishPlan(ServerPlayerEntity p, Upload u) {
        if (!PlacementValidator.canUpload(p)) { CityActions.reply(p, "アップロードの権限がありません", false); return; }
        CityConfig c = CityBuilderMod.config();
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(u.data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            CityActions.reply(p, "JSONを解釈できませんでした", false);
            return;
        }
        if (!root.has("items") || !root.get("items").isJsonArray()) { CityActions.reply(p, "プランの形式ではありません(items がありません)", false); return; }
        if (root.getAsJsonArray("items").size() > c.maxPlanItems) {
            CityActions.reply(p, "項目が多すぎます(最大 " + c.maxPlanItems + ")", false);
            return;
        }
        String key = planKey(p, u.id);
        try {
            Files.createDirectories(PlanLoader.planDir());
            Files.write(PlanLoader.planDir().resolve(key + ".json"), u.data);
        } catch (IOException e) {
            CityActions.reply(p, "保存に失敗しました: " + e.getMessage(), false);
            return;
        }
        CityBuilderMod.LOG.info("{} がプラン {} をアップロードしました ({} bytes)", p.getGameProfile().getName(), key, u.data.length);
        CityActions.reply(p, "プランを保存しました: " + key, true);
        ServerNet.sendCatalog(p);
        PlanSessions.preview((net.minecraft.server.world.ServerWorld) p.world, p, key, p.getBlockPos().down(),
                t -> p.sendMessage(t, false), e -> p.sendMessage(Text.literal("§c[CityBuilder] " + e), false));
    }
}
