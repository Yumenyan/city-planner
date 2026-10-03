package jp.citybuilder;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
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
}
