package jp.citybuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** クライアントからの配置指示を信用せず、サーバーで必ず検証する */
public final class PlacementValidator {
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private PlacementValidator() {}

    public static boolean canUse(ServerPlayerEntity p) {
        CityConfig c = CityBuilderMod.config();
        if (p.hasPermissionLevel(c.permissionLevel)) return true;
        String name = p.getGameProfile().getName();
        for (String s : c.allowedPlayers) if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** .schem アップロードの権限 */
    public static boolean canUpload(ServerPlayerEntity p) {
        CityConfig c = CityBuilderMod.config();
        if (!c.allowUpload || !canUse(p)) return false;
        if (p.hasPermissionLevel(c.uploadPermissionLevel)) return true;
        String name = p.getGameProfile().getName();
        for (String s : c.allowedPlayers) if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static boolean canUse(ServerCommandSource src) {
        if (src.hasPermissionLevel(CityBuilderMod.config().permissionLevel)) return true;
        if (src.getEntity() instanceof ServerPlayerEntity p) return canUse(p);
        return false;
    }

    /** GUI(パケット)経由の配置に対する権限・ゲームモード・連打チェック。問題なければ null */
    public static String checkPlayerRequest(ServerPlayerEntity p) {
        CityConfig c = CityBuilderMod.config();
        if (!canUse(p)) return "権限がありません";
        if (c.requireCreative && !p.isCreative()) return "クリエイティブモードで使用してください";
        int now = p.getServer().getTicks();
        Integer last = LAST.get(p.getUuid());
        if (last != null && now - last < c.cooldownTicks) return "操作が早すぎます";
        LAST.put(p.getUuid(), now);
        return null;
    }

    /**
     * 範囲(両端を含む)の検証。player は null 可(コンソール/コマンド)。
     * 問題なければ null、あればエラー文。
     */
    public static String checkBox(ServerWorld w, ServerPlayerEntity player,
                                  int x1, int y1, int z1, int x2, int y2, int z2) {
        CityConfig c = CityBuilderMod.config();
        String dim = w.getRegistryKey().getValue().toString();
        if (!c.allowedDimensions.isEmpty() && !c.allowedDimensions.contains(dim)) {
            return "このディメンションでは建築できません: " + dim;
        }
        if (y1 < w.getBottomY() || y2 >= w.getTopY()) return "高さがワールドの範囲外です";
        long vol = (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        if (vol > c.maxVolume) return "範囲が大きすぎます";
        if (Math.abs(x1) > 29_999_000 || Math.abs(x2) > 29_999_000 || Math.abs(z1) > 29_999_000 || Math.abs(z2) > 29_999_000) {
            return "座標がワールド境界の外です";
        }
        if (!c.buildAreas.isEmpty()) {
            boolean ok = false;
            for (IntBox b : c.buildAreas) if (b.contains(x1, z1, x2, z2)) { ok = true; break; }
            if (!ok) return "建築可能範囲の外です";
        }
        for (IntBox b : c.protectedAreas) {
            if (b.intersects(x1, z1, x2, z2)) return "保護区域に含まれています";
        }
        if (player != null) {
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            double cx = Math.max(x1, Math.min(px, x2 + 1));
            double cy = Math.max(y1, Math.min(py, y2 + 1));
            double cz = Math.max(z1, Math.min(pz, z2 + 1));
            double d = Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy) + (pz - cz) * (pz - cz));
            if (d > c.maxPlaceDistance) return "遠すぎます(最大 " + c.maxPlaceDistance + " ブロック)";
        }
        return null;
    }
}
