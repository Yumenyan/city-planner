package jp.citybuilder.client;

import jp.citybuilder.NetIds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;

public final class CityBuilderClient implements ClientModInitializer {
    private static final String CAT = "key.categories.citybuilder";
    private static KeyBinding open, rotate, north, south, west, east, up, down;

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.citybuilder." + name, InputUtil.Type.KEYSYM, code, CAT));
    }

    @Override
    public void onInitializeClient() {
        open = key("open", GLFW.GLFW_KEY_B);
        rotate = key("rotate", GLFW.GLFW_KEY_R);
        north = key("nudge_north", GLFW.GLFW_KEY_UP);
        south = key("nudge_south", GLFW.GLFW_KEY_DOWN);
        west = key("nudge_west", GLFW.GLFW_KEY_LEFT);
        east = key("nudge_east", GLFW.GLFW_KEY_RIGHT);
        up = key("nudge_up", GLFW.GLFW_KEY_PAGE_UP);
        down = key("nudge_down", GLFW.GLFW_KEY_PAGE_DOWN);

        ClientPlayNetworking.registerGlobalReceiver(NetIds.CATALOG, (client, handler, buf, sender) -> {
            byte[] bytes = buf.readByteArray();
            client.execute(() -> ClientCatalog.update(new String(bytes, StandardCharsets.UTF_8)));
        });

        ClientPlayNetworking.registerGlobalReceiver(NetIds.PLAN_PREVIEW, (client, handler, buf, sender) -> {
            net.minecraft.network.PacketByteBuf copy = new net.minecraft.network.PacketByteBuf(buf.copy());
            client.execute(() -> PlanPreview.read(copy));
        });

        ClientTickEvents.END_CLIENT_TICK.register(CityBuilderClient::tick);
        ClientTickEvents.END_CLIENT_TICK.register(UploadTask::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlanPreview.clear();
            UploadTask.cancel();
        });

        // 配置モード中の左/右クリックは tick 開始時に消費して処理する(バニラの破壊・設置を防ぐ)
        ClientTickEvents.START_CLIENT_TICK.register(PlacementMode::input);

        HudRenderCallback.EVENT.register(CityBuilderClient::hud);
        PreviewRenderer.register();
    }

    private static void tick(MinecraftClient mc) {
        boolean noScreen = mc.currentScreen == null;
        while (open.wasPressed()) {
            if (noScreen && mc.player != null) {
                mc.setScreen(new CityBuilderScreen());
                noScreen = false;
            }
        }
        boolean building = PlacementMode.kind == PlacementMode.Kind.BUILDING;
        while (rotate.wasPressed()) if (building && noScreen) PlacementMode.rotate();
        while (north.wasPressed()) if (building && noScreen) PlacementMode.nudge(0, 0, -1);
        while (south.wasPressed()) if (building && noScreen) PlacementMode.nudge(0, 0, 1);
        while (west.wasPressed()) if (building && noScreen) PlacementMode.nudge(-1, 0, 0);
        while (east.wasPressed()) if (building && noScreen) PlacementMode.nudge(1, 0, 0);
        while (up.wasPressed()) if (building && noScreen) PlacementMode.nudge(0, 1, 0);
        while (down.wasPressed()) if (building && noScreen) PlacementMode.nudge(0, -1, 0);
        PlacementMode.tick(mc);
    }

    private static void hud(MatrixStack m, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;
        int hy = 8;
        if (UploadTask.active()) {
            mc.textRenderer.drawWithShadow(m, Text.literal("§b" + UploadTask.progress()), 8, hy, 0xFFFFFF);
            hy += 12;
        }
        if (PlanPreview.active()) {
            mc.textRenderer.drawWithShadow(m, Text.literal("§eプランのプレビュー: §f" + PlanPreview.name + " §7(" + PlanPreview.summary + ")"), 8, hy, 0xFFFFFF);
            mc.textRenderer.drawWithShadow(m, Text.literal("§aB→プランタブ または /citybuilder plan confirm§7 で建築 / §c plan cancel§7 で取り消し"), 8, hy + 12, 0xFFFFFF);
            hy += 28;
        }
        if (!PlacementMode.active()) return;
        int x = 8, y = hy;
        String head;
        String help;
        switch (PlacementMode.kind) {
            case BUILDING: {
                BlockPos o = PlacementMode.origin();
                head = "§e建物: §f" + PlacementMode.entry.name + "  §7(" + PlacementMode.rw() + "×" + PlacementMode.entry.h + "×" + PlacementMode.rl()
                        + ")  回転 " + (PlacementMode.rot * 90) + "°" + (o == null ? "" : "  @ " + o.getX() + ", " + o.getY() + ", " + o.getZ());
                help = "右クリック:ここに建てる  Shift+右クリック:やめる  R:回転  矢印:移動  PgUp/PgDn:上下";
                break;
            }
            case ROAD:
                head = "§e道路ツール §7幅 " + PlacementMode.roadWidth + " / " + PlacementMode.roadStyle + " / 点 " + PlacementMode.points.size();
                help = "右クリック:点を追加(同じ点をもう一度で確定)  Shift+右クリック:1点戻す/終了";
                break;
            default:
                head = "§e区画ツール §7" + PlacementMode.areaStyle + (PlacementMode.areaFirst == null ? "" : "  (角を指定済み)");
                help = "右クリック:角を指定→反対の角で確定  Shift+右クリック:取り消し";
        }
        mc.textRenderer.drawWithShadow(m, Text.literal(head), x, y, 0xFFFFFF);
        mc.textRenderer.drawWithShadow(m, Text.literal(help), x, y + 12, 0xCCCCCC);
        if (!ClientCatalog.canBuild) {
            mc.textRenderer.drawWithShadow(m, Text.literal("§c建築権限がありません"), x, y + 24, 0xFF5555);
        } else if (PlacementMode.target == null) {
            mc.textRenderer.drawWithShadow(m, Text.literal("§7地面を見てください"), x, y + 24, 0xAAAAAA);
        }
    }
}
