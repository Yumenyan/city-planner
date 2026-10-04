package jp.citybuilder;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** プレビュー中(未確定)のプランをプレイヤーごとに1つ保持する。 */
public final class PlanSessions {
    /** 確定までの有効時間(tick)。3分。 */
    public static final int EXPIRE_TICKS = 20 * 60 * 3;
    private static final int MAX_SHAPES = 2000;
    private static final Map<UUID, PlanLoader.Prepared> PENDING = new HashMap<>();

    private PlanSessions() {}

    public static void clearAll() { PENDING.clear(); }

    public static void put(ServerPlayerEntity p, PlanLoader.Prepared pr) {
        PENDING.put(pr.owner, pr);
        if (p != null) sendPreview(p, pr);
    }

    /** 保留中のプランを取り出す(期限切れなら null)。 */
    public static PlanLoader.Prepared peek(UUID owner, int nowTick) {
        PlanLoader.Prepared pr = PENDING.get(owner);
        if (pr != null && nowTick - pr.createdTick > EXPIRE_TICKS) {
            PENDING.remove(owner);
            return null;
        }
        return pr;
    }

    public static boolean cancel(ServerPlayerEntity p, UUID owner) {
        boolean had = PENDING.remove(owner) != null;
        if (p != null) sendClear(p);
        return had;
    }

    /** 確定してジョブを積む。 */
    public static PlanLoader.Prepared confirm(ServerPlayerEntity p, UUID owner, int nowTick) throws IOException {
        PlanLoader.Prepared pr = peek(owner, nowTick);
        if (pr == null) throw new IOException("確定できるプランがありません(期限切れの可能性: /citybuilder plan load をやり直してください)");
        PlanLoader.submit(pr);
        PENDING.remove(owner);
        if (p != null) sendClear(p);
        return pr;
    }

    public static void sendPreview(ServerPlayerEntity p, PlanLoader.Prepared pr) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(true);
        buf.writeString(pr.name, 64);
        PlanLoader.Result r = pr.result;
        String summary = "建物 " + r.buildings + " / 道路 " + r.roads + " / 区画 " + r.areas;
        buf.writeString(summary, 200);
        buf.writeBlockPos(pr.origin);
        if (r.bounds != null) {
            buf.writeBoolean(true);
            for (int v : r.bounds) buf.writeInt(v);
        } else {
            buf.writeBoolean(false);
        }
        List<int[]> shapes = pr.shapes;
        int n = Math.min(MAX_SHAPES, shapes.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            int[] s = shapes.get(i);
            buf.writeByte(s[0]);
            for (int k = 1; k < 7; k++) buf.writeInt(s[k]);
        }
        ServerPlayNetworking.send(p, NetIds.PLAN_PREVIEW, buf);
    }

    public static void sendClear(ServerPlayerEntity p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(false);
        ServerPlayNetworking.send(p, NetIds.PLAN_PREVIEW, buf);
    }

    /** プランを検証してプレビューを出す(まだ建てない)。コマンド・GUI・アップロードの共通処理。 */
    public static boolean preview(ServerWorld w, ServerPlayerEntity p, String name, BlockPos origin,
                                  Consumer<Text> out, Consumer<String> err) {
        try {
            PlanLoader.Prepared pr = PlanLoader.prepare(w, p, name, origin);
            PlanLoader.Result r = pr.result;
            for (String e : r.errors) err.accept("  スキップ " + e);
            if (pr.jobs.isEmpty()) { err.accept("実行できる項目がありません (" + r.skipped + " 件をスキップ)"); return false; }
            put(p, pr);
            String range = r.bounds == null ? "" : " / 範囲 X " + r.bounds[0] + "〜" + r.bounds[2] + ", Z " + r.bounds[1] + "〜" + r.bounds[3]
                    + " (" + (r.bounds[2] - r.bounds[0] + 1) + "x" + (r.bounds[3] - r.bounds[1] + 1) + ")";
            out.accept(Text.literal("プラン '" + name + "' のプレビュー: 建物 " + r.buildings + " / 道路 " + r.roads + " / 区画 " + r.areas
                    + " (スキップ " + r.skipped + ") / 約 " + r.estimate + " ブロック / 原点 " + origin.toShortString() + range));
            Text confirm = Text.literal("[確定して建築]").styled(st -> st.withColor(Formatting.GREEN).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/citybuilder plan confirm")));
            Text cancel = Text.literal("[取り消し]").styled(st -> st.withColor(Formatting.RED)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/citybuilder plan cancel")));
            out.accept(Text.literal("黄色の枠が街の範囲です(青=建物 白=道路 緑=区画)。 ").append(confirm).append(Text.literal(" ")).append(cancel)
                    .append(Text.literal(" ※3分で失効 / B→プランタブからも操作できます")));
            return true;
        } catch (IOException e) {
            err.accept(e.getMessage());
            return false;
        }
    }

    /** GUI(パケット)からのプラン操作。0=足元を原点にプレビュー 1=確定 2=取り消し */
    public static void onAction(ServerPlayerEntity p, int action, String name) {
        if (!PlacementValidator.canUse(p)) { CityActions.reply(p, "権限がありません", false); return; }
        Consumer<Text> out = t -> p.sendMessage(t, false);
        Consumer<String> err = e -> p.sendMessage(Text.literal("§c[CityBuilder] " + e), false);
        UUID owner = p.getUuid();
        if (action == 0) {
            if (!PlanLoader.validName(name)) { CityActions.reply(p, "プラン名が不正です", false); return; }
            preview((ServerWorld) p.world, p, name, p.getBlockPos().down(), out, err);
        } else if (action == 1) {
            String e = PlacementValidator.checkPlayerRequest(p);
            if (e != null) { CityActions.reply(p, e, false); return; }
            try {
                PlanLoader.Prepared pr = confirm(p, owner, p.getServer().getTicks());
                CityActions.reply(p, "プラン '" + pr.name + "' を開始(undo 1回で一括して戻せます)", true);
            } catch (IOException ex) {
                CityActions.reply(p, ex.getMessage(), false);
            }
        } else if (action == 2) {
            CityActions.reply(p, cancel(p, owner) ? "プレビューを取り消しました" : "保留中のプランはありません", true);
        }
    }
}
