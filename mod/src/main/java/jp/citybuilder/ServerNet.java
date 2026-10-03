package jp.citybuilder;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;

/** クライアント⇔サーバーの通信。クライアントには建物IDと寸法などのカタログ情報だけを送る。 */
public final class ServerNet {
    private ServerNet() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(NetIds.REQUEST_CATALOG, (server, player, handler, buf, sender) ->
                server.execute(() -> sendCatalog(player)));

        ServerPlayNetworking.registerGlobalReceiver(NetIds.UNDO, (server, player, handler, buf, sender) ->
                server.execute(() -> CityActions.onUndo(player)));

        ServerPlayNetworking.registerGlobalReceiver(NetIds.PLACE, (server, player, handler, buf, sender) -> {
            try {
                String id = buf.readString(64);
                BlockPos pos = buf.readBlockPos();
                int rot = buf.readByte();
                server.execute(() -> CityActions.onPlace(player, id, pos, rot));
            } catch (RuntimeException e) {
                CityBuilderMod.LOG.warn("不正な place パケット: {}", e.toString());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(NetIds.ROAD, (server, player, handler, buf, sender) -> {
            try {
                int width = buf.readVarInt();
                boolean sidewalk = buf.readBoolean();
                String style = buf.readString(32);
                String lines = buf.readString(16);
                int n = buf.readVarInt();
                if (n < 2 || n > 256) return;
                int[] px = new int[n], pz = new int[n];
                int y = 0;
                for (int i = 0; i < n; i++) {
                    BlockPos p = buf.readBlockPos();
                    px[i] = p.getX(); pz[i] = p.getZ();
                    if (i == 0) y = p.getY();
                }
                final int fy = y;
                server.execute(() -> CityActions.onRoad(player, px, pz, fy, width, sidewalk, style, lines));
            } catch (RuntimeException e) {
                CityBuilderMod.LOG.warn("不正な road パケット: {}", e.toString());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(NetIds.AREA, (server, player, handler, buf, sender) -> {
            try {
                BlockPos a = buf.readBlockPos();
                BlockPos b = buf.readBlockPos();
                String style = buf.readString(48);
                server.execute(() -> CityActions.onArea(player, a.getX(), a.getZ(), b.getX(), b.getZ(), a.getY(), style));
            } catch (RuntimeException e) {
                CityBuilderMod.LOG.warn("不正な area パケット: {}", e.toString());
            }
        });
    }

    public static void sendCatalog(ServerPlayerEntity p) {
        CityConfig c = CityBuilderMod.config();
        String json = CityBuilderMod.catalog().toClientJson(PlacementValidator.canUse(p), c.maxPlaceDistance);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(json.getBytes(StandardCharsets.UTF_8));
        ServerPlayNetworking.send(p, NetIds.CATALOG, buf);
    }

    public static void broadcastCatalog(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendCatalog(p);
    }
}
