package jp.citybuilder.client;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** サーバーから届いたプラン範囲プレビュー。confirm / cancel / 一定時間で消える。 */
public final class PlanPreview {
    public static final List<int[]> shapes = new ArrayList<>();
    public static int[] bounds;
    public static BlockPos origin;
    public static int baseY;
    public static String name = "";
    public static String summary = "";

    private PlanPreview() {}

    private static long receivedAt;

    public static boolean active() {
        if (shapes.isEmpty() && bounds == null) return false;
        if (System.currentTimeMillis() - receivedAt > 190_000L) { clear(); return false; } // サーバー側の期限(3分)に合わせる
        return true;
    }

    public static void clear() {
        shapes.clear();
        bounds = null;
        origin = null;
        name = "";
        summary = "";
    }

    /** S2C plan_preview: boolean active, [name, summary, origin, bounds?, shapes] */
    public static void read(PacketByteBuf buf) {
        clear();
        if (!buf.readBoolean()) return;
        receivedAt = System.currentTimeMillis();
        name = buf.readString(64);
        summary = buf.readString(200);
        origin = buf.readBlockPos();
        baseY = origin.getY();
        if (buf.readBoolean()) bounds = new int[]{buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()};
        int n = buf.readVarInt();
        for (int i = 0; i < n && i < 4000; i++) {
            shapes.add(new int[]{buf.readByte(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()});
        }
    }
}
