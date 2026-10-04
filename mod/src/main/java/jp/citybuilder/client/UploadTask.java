package jp.citybuilder.client;

import jp.citybuilder.NetIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * .schem を 30000 バイトずつ、1tickに1つずつサーバーへ送る(1パケット約32KBの上限を避けるため)。
 * 結果はサーバーがアクションバーに表示し、完了するとカタログが更新される。
 */
public final class UploadTask {
    public static final int CHUNK = 30000;
    private static byte[] data;
    private static int pos;
    private static String label = "";

    private UploadTask() {}

    public static boolean active() { return data != null; }

    public static String progress() {
        return data == null ? "" : label + " を送信中 " + (pos * 100L / data.length) + "% (" + pos / 1000 + "/" + data.length / 1000 + " KB)";
    }

    public static void start(String kind, String id, String name, String category, String desc, byte[] bytes) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeString(kind, 8);
        b.writeString(id, 64);
        b.writeString(name, 64);
        b.writeString(category, 32);
        b.writeString(desc, 200);
        b.writeVarInt(bytes.length);
        ClientPlayNetworking.send(NetIds.UPLOAD_BEGIN, b);
        data = bytes;
        pos = 0;
        label = name;
    }

    public static void cancel() { data = null; pos = 0; }

    public static void tick(MinecraftClient mc) {
        if (data == null) return;
        if (mc.player == null || mc.getNetworkHandler() == null) { cancel(); return; }
        int n = Math.min(CHUNK, data.length - pos);
        PacketByteBuf b = PacketByteBufs.create();
        b.writeByteArray(Arrays.copyOfRange(data, pos, pos + n));
        ClientPlayNetworking.send(NetIds.UPLOAD_CHUNK, b);
        pos += n;
        if (pos >= data.length) {
            data = null;
            mc.player.sendMessage(Text.literal("[CityBuilder] 送信完了。サーバーの検証結果を待っています…"), true);
        }
    }
}
