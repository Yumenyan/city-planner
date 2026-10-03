package jp.citybuilder.client;

import jp.citybuilder.NetIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** 配置モード(建物 / 道路 / 区画)の状態。見ている地表ブロックを基準に、プレビューを追従させる。 */
public final class PlacementMode {
    public enum Kind { NONE, BUILDING, ROAD, AREA }

    public static Kind kind = Kind.NONE;
    public static ClientCatalog.Entry entry;
    public static int rot, dx, dy, dz;
    public static BlockPos target;

    public static int roadWidth = 7;
    public static String roadStyle = "asphalt";
    public static String lines = "dashed";
    public static final List<BlockPos> points = new ArrayList<>();

    public static String areaStyle = "grass";
    public static BlockPos areaFirst;

    private PlacementMode() {}

    public static boolean active() { return kind != Kind.NONE; }

    public static void startBuilding(ClientCatalog.Entry e) {
        kind = Kind.BUILDING; entry = e; rot = 0; dx = dy = dz = 0; target = null;
    }

    public static void startRoad() {
        kind = Kind.ROAD; points.clear(); target = null;
    }

    public static void startArea() {
        kind = Kind.AREA; areaFirst = null; target = null;
    }

    public static void cancel() {
        kind = Kind.NONE; entry = null; points.clear(); areaFirst = null; target = null;
    }

    public static int rw() { return entry == null ? 1 : ((rot & 1) == 0 ? entry.w : entry.l); }
    public static int rl() { return entry == null ? 1 : ((rot & 1) == 0 ? entry.l : entry.w); }

    /** 建物の最小コーナー(サーバーに送る位置) */
    public static BlockPos origin() {
        if (target == null) return null;
        return new BlockPos(target.getX() - rw() / 2 + dx, target.getY() + dy, target.getZ() - rl() / 2 + dz);
    }

    public static void rotate() { rot = (rot + 1) & 3; }
    public static void nudge(int x, int y, int z) { dx += x; dy += y; dz += z; }

    public static void tick(MinecraftClient mc) {
        if (!active() || mc.player == null) return;
        HitResult hit = mc.player.raycast(ClientCatalog.maxDistance, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) target = ((BlockHitResult) hit).getBlockPos();
        else target = null;
    }

    /**
     * クライアントtick開始時に呼ぶ。配置モード中は攻撃/使用キーの押下をここで消費して、
     * バニラのブロック破壊・設置を起こさせず、左クリック=確定 / 右クリック=終了 として扱う。
     */
    public static void input(MinecraftClient mc) {
        if (!active() || mc.player == null || mc.currentScreen != null) return;
        // 左クリックはブロック破壊を防ぐために消費するだけ
        while (mc.options.attackKey.wasPressed()) { /* 何もしない */ }
        // 右クリック=選択/指定、Shift+右クリック=取り消し
        while (mc.options.useKey.wasPressed()) {
            if (!active()) break;
            if (Screen.hasShiftDown()) cancelStep(mc);
            else confirm(mc);
        }
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[CityBuilder] " + s), true);
    }

    private static void confirm(MinecraftClient mc) {
        if (!ClientCatalog.canBuild) { msg(mc, "このサーバーでは建築する権限がありません"); return; }
        if (target == null) { msg(mc, "地面を見てください"); return; }
        switch (kind) {
            case BUILDING: {
                BlockPos o = origin();
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(entry.id, 64);
                buf.writeBlockPos(o);
                buf.writeByte(rot);
                ClientPlayNetworking.send(NetIds.PLACE, buf);
                break;
            }
            case ROAD: {
                BlockPos last = points.isEmpty() ? null : points.get(points.size() - 1);
                if (last != null && last.getX() == target.getX() && last.getZ() == target.getZ()) {
                    sendRoad(); // 同じ点をもう一度指定 = 確定
                } else if (points.size() < 256) {
                    points.add(target);
                }
                break;
            }
            case AREA:
                if (areaFirst == null) {
                    areaFirst = target;
                } else {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBlockPos(areaFirst);
                    buf.writeBlockPos(new BlockPos(target.getX(), areaFirst.getY(), target.getZ()));
                    buf.writeString(areaStyle, 48);
                    ClientPlayNetworking.send(NetIds.AREA, buf);
                    areaFirst = null;
                }
                break;
            default:
        }
    }

    private static void sendRoad() {
        if (points.size() < 2) { points.clear(); return; }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(roadWidth);
        buf.writeBoolean(true);
        buf.writeString(roadStyle, 32);
        buf.writeString(lines, 16);
        buf.writeVarInt(points.size());
        int y = points.get(0).getY();
        for (BlockPos p : points) buf.writeBlockPos(new BlockPos(p.getX(), y, p.getZ()));
        ClientPlayNetworking.send(NetIds.ROAD, buf);
        points.clear();
    }

    /** Shift+右クリック: 1手戻す。戻すものが無ければ配置モードを終了 */
    private static void cancelStep(MinecraftClient mc) {
        switch (kind) {
            case ROAD:
                if (!points.isEmpty()) points.remove(points.size() - 1);
                else cancel();
                break;
            case AREA:
                if (areaFirst != null) areaFirst = null;
                else cancel();
                break;
            default:
                cancel();
        }
    }
}
