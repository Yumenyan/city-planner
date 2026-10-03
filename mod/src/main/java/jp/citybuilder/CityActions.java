package jp.citybuilder;

import jp.citybuilder.job.AreaJob;
import jp.citybuilder.job.RoadJob;
import jp.citybuilder.job.Styles;
import jp.citybuilder.job.TemplateJob;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** パケット・コマンド・プランから共通で呼ぶ「配置API」。検証して問題なければジョブを積む。エラー文 or null を返す。 */
public final class CityActions {
    public static final UUID CONSOLE = new UUID(0L, 0L);

    private CityActions() {}

    public static UUID ownerOf(ServerPlayerEntity p) { return p == null ? CONSOLE : p.getUuid(); }

    public static String placeTemplate(ServerWorld w, ServerPlayerEntity p, String id, BlockPos pos, int rot) {
        BuildingCatalog cat = CityBuilderMod.catalog();
        Template t = cat.template(id);
        if (t == null) return "不明なテンプレート: " + id;
        if (rot < 0 || rot > 3) return "回転が不正です";
        int rw = t.rw(rot), rl = t.rl(rot);
        String err = PlacementValidator.checkBox(w, p, pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + rw - 1, pos.getY() + t.h - 1, pos.getZ() + rl - 1);
        if (err != null) return err;
        if (t.volume() > CityBuilderMod.config().maxVolume) return "建物が大きすぎます";
        if (!CityBuilderMod.jobs().canAccept(t.volume())) return "作業キューが一杯です。完了を待ってください";
        CityConfig c = CityBuilderMod.config();
        CityBuilderMod.jobs().submit(new TemplateJob(ownerOf(p), w, t, pos, rot, c.templateFoundation, c.foundationDepth));
        return null;
    }

    public static String road(ServerWorld w, ServerPlayerEntity p, int[] px, int[] pz, int y,
                              int width, boolean sidewalk, String styleName, String lines) {
        Styles.Road style = Styles.road(styleName);
        if (style == null) return "不明な道路スタイル: " + styleName;
        if (px.length < 2 || px.length > 256) return "道路の点は2〜256個にしてください";
        if (width < 1 || width > 31) return "幅は1〜31にしてください";
        if (lines == null) lines = "dashed";
        if (!List.of("none", "dashed", "solid", "double").contains(lines)) return "不明な線スタイル: " + lines;
        CityConfig c = CityBuilderMod.config();
        int[] b = RoadJob.bounds(px, pz, width, sidewalk);
        String err = PlacementValidator.checkBox(w, p, b[0], y, b[1], b[2], y, b[3]);
        if (err != null) return err;
        long area = (long) (b[2] - b[0] + 1) * (b[3] - b[1] + 1);
        if (area > c.maxVolume) return "道路の範囲が大きすぎます";
        if (!CityBuilderMod.jobs().canAccept(area)) return "作業キューが一杯です。完了を待ってください";
        CityBuilderMod.jobs().submit(new RoadJob(ownerOf(p), w, px, pz, width, sidewalk, style, lines, y,
                c.roadClearAbove, c.roadFoundation, c.foundationDepth));
        return null;
    }

    public static String area(ServerWorld w, ServerPlayerEntity p, int x1, int z1, int x2, int z2, int y, String styleName) {
        Block block = Styles.area(styleName);
        if (block == null) return "不明な区画スタイル: " + styleName;
        int ax1 = Math.min(x1, x2), ax2 = Math.max(x1, x2), az1 = Math.min(z1, z2), az2 = Math.max(z1, z2);
        String err = PlacementValidator.checkBox(w, p, ax1, y, az1, ax2, y, az2);
        if (err != null) return err;
        long area = (long) (ax2 - ax1 + 1) * (az2 - az1 + 1);
        if (area > CityBuilderMod.config().maxVolume) return "区画が大きすぎます";
        if (!CityBuilderMod.jobs().canAccept(area)) return "作業キューが一杯です。完了を待ってください";
        CityConfig c = CityBuilderMod.config();
        CityBuilderMod.jobs().submit(new AreaJob(ownerOf(p), w, ax1, az1, ax2, az2, y, block,
                c.roadClearAbove, c.roadFoundation, c.foundationDepth));
        return null;
    }

    // ---- パケット経由(プレイヤー操作) ----

    private static void reply(ServerPlayerEntity p, String msg, boolean ok) {
        p.sendMessage(Text.literal((ok ? "§a" : "§c") + "[CityBuilder] " + msg), true);
    }

    public static void onPlace(ServerPlayerEntity p, String id, BlockPos pos, int rot) {
        String err = PlacementValidator.checkPlayerRequest(p);
        if (err == null) err = placeTemplate((ServerWorld) p.world, p, id, pos, rot);
        if (err != null) reply(p, err, false);
        else reply(p, "配置を開始: " + id, true);
    }

    public static void onRoad(ServerPlayerEntity p, int[] px, int[] pz, int y, int width, boolean sidewalk, String style, String lines) {
        String err = PlacementValidator.checkPlayerRequest(p);
        if (err == null) err = road((ServerWorld) p.world, p, px, pz, y, width, sidewalk, style, lines);
        if (err != null) reply(p, err, false);
        else reply(p, "道路の作成を開始", true);
    }

    public static void onArea(ServerPlayerEntity p, int x1, int z1, int x2, int z2, int y, String style) {
        String err = PlacementValidator.checkPlayerRequest(p);
        if (err == null) err = area((ServerWorld) p.world, p, x1, z1, x2, z2, y, style);
        if (err != null) reply(p, err, false);
        else reply(p, "区画の作成を開始", true);
    }

    public static void onUndo(ServerPlayerEntity p) {
        if (!PlacementValidator.canUse(p)) { reply(p, "権限がありません", false); return; }
        if (CityBuilderMod.jobs().undo(p.getUuid())) reply(p, "直前の配置を元に戻します", true);
        else reply(p, "戻せる履歴がありません", false);
    }
}
