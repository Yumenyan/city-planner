package jp.citybuilder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /citybuilder reload | list [category] | info <id> | place <id> <pos> [rotation]
 *              | road <from> <to> [width] [style] | area <from> <to> <style>
 *              | undo | cancel | status | plan list | plan load <name> [origin] (プレビュー) | plan confirm | plan cancel
 */
public final class CityCommands {
    private CityCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
    }

    private static UUID ownerOf(ServerCommandSource s) {
        return s.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : CityActions.CONSOLE;
    }

    private static ServerPlayerEntity playerOf(ServerCommandSource s) {
        return s.getEntity() instanceof ServerPlayerEntity p ? p : null;
    }

    private static int fail(ServerCommandSource s, String msg) {
        s.sendError(Text.literal(msg));
        return 0;
    }

    private static int ok(ServerCommandSource s, String msg) {
        s.sendFeedback(Text.literal(msg), false);
        return 1;
    }

    private static int rotArg(int deg) {
        return deg >= 4 ? (deg / 90) & 3 : deg & 3;
    }

    private static void build(CommandDispatcher<ServerCommandSource> d) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("citybuilder")
                .requires(PlacementValidator::canUse);

        root.then(CommandManager.literal("reload").executes(ctx -> {
            CityBuilderMod.reload();
            ServerNet.broadcastCatalog(ctx.getSource().getServer());
            return ok(ctx.getSource(), "再読み込みしました。建物数: " + CityBuilderMod.catalog().size());
        }));

        root.then(CommandManager.literal("list")
                .executes(ctx -> list(ctx.getSource(), null))
                .then(CommandManager.argument("category", StringArgumentType.greedyString())
                        .suggests((c, b) -> CommandSource.suggestMatching(categories(), b))
                        .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "category")))));

        root.then(CommandManager.literal("info")
                .then(CommandManager.argument("template", StringArgumentType.word())
                        .suggests((c, b) -> CommandSource.suggestMatching(ids(), b))
                        .executes(ctx -> {
                            BuildingCatalog.Entry e = CityBuilderMod.catalog().get(StringArgumentType.getString(ctx, "template"));
                            if (e == null) return fail(ctx.getSource(), "不明なテンプレートです");
                            return ok(ctx.getSource(), e.id + " / " + e.name + " [" + e.category + "] "
                                    + e.size[0] + "x" + e.size[1] + "x" + e.size[2] + (e.floors > 0 ? " " + e.floors + "階" : "")
                                    + " " + e.description);
                        })));

        root.then(CommandManager.literal("place")
                .then(CommandManager.argument("template", StringArgumentType.word())
                        .suggests((c, b) -> CommandSource.suggestMatching(ids(), b))
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(ctx -> place(ctx, 0))
                                .then(CommandManager.argument("rotation", IntegerArgumentType.integer(0, 360))
                                        .executes(ctx -> place(ctx, rotArg(IntegerArgumentType.getInteger(ctx, "rotation"))))))));

        root.then(CommandManager.literal("road")
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                .executes(ctx -> road(ctx, 7, "asphalt"))
                                .then(CommandManager.argument("width", IntegerArgumentType.integer(3, 31))
                                        .executes(ctx -> road(ctx, IntegerArgumentType.getInteger(ctx, "width"), "asphalt"))
                                        .then(CommandManager.argument("style", StringArgumentType.word())
                                                .suggests((c, b) -> CommandSource.suggestMatching(jp.citybuilder.job.Styles.ROADS.keySet(), b))
                                                .executes(ctx -> road(ctx, IntegerArgumentType.getInteger(ctx, "width"),
                                                        StringArgumentType.getString(ctx, "style"))))))));

        root.then(CommandManager.literal("area")
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("style", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(jp.citybuilder.job.Styles.AREAS.keySet(), b))
                                        .executes(CityCommands::area)))));

        root.then(CommandManager.literal("undo").executes(ctx -> {
            if (CityBuilderMod.jobs().undo(ownerOf(ctx.getSource()))) return ok(ctx.getSource(), "直前の作業を元に戻します");
            return fail(ctx.getSource(), "戻せる履歴がありません");
        }));

        root.then(CommandManager.literal("cancel").executes(ctx -> {
            int n = CityBuilderMod.jobs().cancel(ownerOf(ctx.getSource()));
            return ok(ctx.getSource(), n + " 件の作業を取り消しました(途中までの変更は undo で戻せます)");
        }));

        root.then(CommandManager.literal("status").executes(ctx -> ok(ctx.getSource(),
                "キュー: " + CityBuilderMod.jobs().queued() + " 件 / 残り約 " + CityBuilderMod.jobs().pending()
                        + " ブロック / あなたの履歴: " + CityBuilderMod.jobs().historySize(ownerOf(ctx.getSource())))));

        root.then(CommandManager.literal("plan")
                .then(CommandManager.literal("list").executes(ctx -> {
                    List<String> l = PlanLoader.list();
                    return ok(ctx.getSource(), l.isEmpty() ? "プランがありません (config/citybuilder/plans/*.json)" : "プラン: " + String.join(", ", l));
                }))
                .then(CommandManager.literal("confirm").executes(CityCommands::planConfirm))
                .then(CommandManager.literal("cancel").executes(ctx -> {
                    ServerCommandSource s = ctx.getSource();
                    boolean had = PlanSessions.cancel(playerOf(s), ownerOf(s));
                    return had ? ok(s, "プランのプレビューを取り消しました") : fail(s, "保留中のプランはありません");
                }))
                .then(CommandManager.literal("load")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(PlanLoader.list(), b))
                                .executes(ctx -> plan(ctx, null))
                                .then(CommandManager.argument("origin", BlockPosArgumentType.blockPos())
                                        .executes(ctx -> plan(ctx, BlockPosArgumentType.getBlockPos(ctx, "origin")))))));

        d.register(root);
    }

    private static List<String> ids() {
        List<String> r = new ArrayList<>();
        for (BuildingCatalog.Entry e : CityBuilderMod.catalog().all()) r.add(e.id);
        return r;
    }

    private static Set<String> categories() {
        Set<String> r = new LinkedHashSet<>();
        for (BuildingCatalog.Entry e : CityBuilderMod.catalog().all()) r.add(e.category);
        return r;
    }

    private static int list(ServerCommandSource s, String category) {
        StringBuilder sb = new StringBuilder();
        for (BuildingCatalog.Entry e : CityBuilderMod.catalog().all()) {
            if (category != null && !e.category.equals(category)) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.id);
        }
        if (sb.length() == 0) return fail(s, "該当する建物がありません。カテゴリ: " + String.join(", ", categories()));
        return ok(s, (category == null ? "全建物: " : category + ": ") + sb);
    }

    private static int place(CommandContext<ServerCommandSource> ctx, int rot) throws CommandSyntaxException {
        ServerCommandSource s = ctx.getSource();
        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
        String id = StringArgumentType.getString(ctx, "template");
        String err = CityActions.placeTemplate(s.getWorld(), playerOf(s), id, pos, rot);
        return err != null ? fail(s, err) : ok(s, "配置を開始: " + id + " @ " + pos.toShortString() + " 回転 " + rot * 90);
    }

    private static int road(CommandContext<ServerCommandSource> ctx, int width, String style) throws CommandSyntaxException {
        ServerCommandSource s = ctx.getSource();
        BlockPos a = BlockPosArgumentType.getBlockPos(ctx, "from");
        BlockPos b = BlockPosArgumentType.getBlockPos(ctx, "to");
        String err = CityActions.road(s.getWorld(), playerOf(s), new int[]{a.getX(), b.getX()}, new int[]{a.getZ(), b.getZ()},
                a.getY(), width, true, style, "dashed");
        return err != null ? fail(s, err) : ok(s, "道路の作成を開始 (幅 " + width + ", " + style + ")");
    }

    private static int area(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource s = ctx.getSource();
        BlockPos a = BlockPosArgumentType.getBlockPos(ctx, "from");
        BlockPos b = BlockPosArgumentType.getBlockPos(ctx, "to");
        String style = StringArgumentType.getString(ctx, "style");
        String err = CityActions.area(s.getWorld(), playerOf(s), a.getX(), a.getZ(), b.getX(), b.getZ(), a.getY(), style);
        return err != null ? fail(s, err) : ok(s, "区画の作成を開始 (" + style + ")");
    }

    private static int plan(CommandContext<ServerCommandSource> ctx, BlockPos origin) {
        ServerCommandSource s = ctx.getSource();
        ServerPlayerEntity p = playerOf(s);
        if (origin == null) {
            if (p == null) return fail(s, "コンソールからは origin 座標を指定してください");
            origin = p.getBlockPos().down(); // 立っているブロック(地表)
        }
        String name = StringArgumentType.getString(ctx, "name");
        try {
            PlanLoader.Prepared pr = PlanLoader.prepare(s.getWorld(), p, name, origin);
            PlanLoader.Result r = pr.result;
            for (String e : r.errors) s.sendError(Text.literal("  スキップ " + e));
            if (pr.jobs.isEmpty()) return fail(s, "実行できる項目がありません (" + r.skipped + " 件をスキップ)");
            PlanSessions.put(p, pr);
            String range = r.bounds == null ? "" : " / 範囲 X " + r.bounds[0] + "〜" + r.bounds[2] + ", Z " + r.bounds[1] + "〜" + r.bounds[3]
                    + " (" + (r.bounds[2] - r.bounds[0] + 1) + "x" + (r.bounds[3] - r.bounds[1] + 1) + ")";
            s.sendFeedback(Text.literal("プラン '" + name + "' のプレビュー: 建物 " + r.buildings + " / 道路 " + r.roads + " / 区画 " + r.areas
                    + " (スキップ " + r.skipped + ") / 約 " + r.estimate + " ブロック / 原点 " + origin.toShortString() + range), false);
            Text confirm = Text.literal("[確定して建築]").styled(st -> st.withColor(Formatting.GREEN).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/citybuilder plan confirm")));
            Text cancel = Text.literal("[取り消し]").styled(st -> st.withColor(Formatting.RED)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/citybuilder plan cancel")));
            s.sendFeedback(Text.literal("黄色の枠が街の範囲です(青=建物 白=道路 緑=区画)。 ").append(confirm).append(Text.literal(" ")).append(cancel)
                    .append(Text.literal(" ※3分で期限切れ")), false);
            return 1;
        } catch (IOException e) {
            return fail(s, e.getMessage());
        }
    }

    private static int planConfirm(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        try {
            PlanLoader.Prepared pr = PlanSessions.confirm(playerOf(s), ownerOf(s), s.getServer().getTicks());
            return ok(s, "プラン '" + pr.name + "' を開始: 約 " + pr.result.estimate + " ブロック (/citybuilder undo で一括して戻せます)");
        } catch (IOException e) {
            return fail(s, e.getMessage());
        }
    }
}
