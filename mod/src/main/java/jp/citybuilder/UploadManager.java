package jp.citybuilder;

import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
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
        String id, name, category, desc;
        byte[] data;
        int pos;
    }

    private static final Map<UUID, Upload> ACTIVE = new HashMap<>();

    private UploadManager() {}

    public static void clear() { ACTIVE.clear(); }

    private static String clean(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("[\\p{Cntrl}]", "").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static void begin(ServerPlayerEntity p, String id, String name, String category, String desc, int total) {
        ACTIVE.remove(p.getUuid());
        CityConfig c = CityBuilderMod.config();
        if (!PlacementValidator.canUpload(p)) { CityActions.reply(p, "アップロードの権限がありません", false); return; }
        if (!BuildingCatalog.validId(id)) { CityActions.reply(p, "IDは半角英数字と _ - の48文字以内にしてください", false); return; }
        if (total <= 0 || total > c.maxUploadBytes) {
            CityActions.reply(p, "ファイルが大きすぎます(最大 " + c.maxUploadBytes / 1000 + " KB)", false);
            return;
        }
        if (CityBuilderMod.catalog().has(id) && !c.allowUploadOverwrite) {
            CityActions.reply(p, "同じIDの建物が既にあります: " + id, false);
            return;
        }
        Upload u = new Upload();
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
        finish(p, u);
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
}
