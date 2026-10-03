package jp.citybuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jp.citybuilder.job.AreaJob;
import jp.citybuilder.job.GroupJob;
import jp.citybuilder.job.Job;
import jp.citybuilder.job.RoadJob;
import jp.citybuilder.job.Styles;
import jp.citybuilder.job.TemplateJob;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * WebUIが書き出した city.json(都市計画)を読み込んでジョブ化する。
 * 座標は origin(地表ブロック)からのオフセット。building の x,z は回転後フットプリントの最小コーナー。
 */
public final class PlanLoader {
    private static final Pattern NAME_RE = Pattern.compile("[A-Za-z0-9_\\-]+");

    public static final class Result {
        public int accepted, skipped;
        public long estimate;
        public int buildings, roads, areas;
        /** 全体の範囲 {minX, minZ, maxX, maxZ}。受け付けた項目が無ければ null */
        public int[] bounds;
        public final List<String> errors = new ArrayList<>();
    }

    /** プレビュー待ちのプラン。confirm でジョブ化する。 */
    public static final class Prepared {
        public String name;
        public BlockPos origin;
        public ServerWorld world;
        public java.util.UUID owner;
        public long createdTick;
        public final List<Job> jobs = new ArrayList<>();
        /** プレビュー用の箱 {種類(0建物/1道路/2区画), x1,y1,z1,x2,y2,z2} */
        public final List<int[]> shapes = new ArrayList<>();
        public final Result result = new Result();
    }

    private PlanLoader() {}

    public static Path planDir() { return CityConfig.dir().resolve("plans"); }

    public static List<String> list() {
        List<String> r = new ArrayList<>();
        try (Stream<Path> s = Files.list(planDir())) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted()
                    .forEach(n -> r.add(n.substring(0, n.length() - 5)));
        } catch (IOException ignored) {}
        return r;
    }

    private static int num(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        return (e == null || !e.isJsonPrimitive()) ? def : e.getAsInt();
    }

    private static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return (e == null || !e.isJsonPrimitive()) ? def : e.getAsString();
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        return (e == null || !e.isJsonPrimitive()) ? def : e.getAsBoolean();
    }

    private static int prio(String type) {
        return "area".equals(type) ? 0 : "road".equals(type) ? 1 : 2;
    }

    /** プランを検証してジョブを作る(まだ実行しない)。 */
    public static Prepared prepare(ServerWorld w, ServerPlayerEntity p, String name, BlockPos origin) throws IOException {
        if (!NAME_RE.matcher(name).matches()) throw new IOException("プラン名が不正です");
        Path file = planDir().resolve(name + ".json");
        if (!Files.exists(file)) throw new IOException("プランが見つかりません: " + name + ".json");
        JsonObject root;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("JSONを解釈できません: " + e.getMessage());
        }
        int baseY = root.has("groundY") ? num(root, "groundY", origin.getY()) : origin.getY();
        JsonArray items = root.has("items") ? root.getAsJsonArray("items") : new JsonArray();
        List<JsonObject> sorted = new ArrayList<>();
        for (JsonElement e : items) if (e.isJsonObject()) sorted.add(e.getAsJsonObject());
        sorted.sort(Comparator.comparingInt(o -> prio(str(o, "type", ""))));

        Prepared pr = new Prepared();
        pr.name = name; pr.origin = origin; pr.world = w; pr.owner = CityActions.ownerOf(p);
        pr.createdTick = w.getServer().getTicks();
        Result res = pr.result;
        List<Job> jobs = pr.jobs;
        CityConfig c = CityBuilderMod.config();
        BuildingCatalog cat = CityBuilderMod.catalog();
        int ox = origin.getX(), oz = origin.getZ();
        int idx = 0;
        for (JsonObject o : sorted) {
            idx++;
            String type = str(o, "type", "");
            String err = null;
            try {
                int y = baseY + num(o, "dy", 0);
                switch (type) {
                    case "building": {
                        String id = str(o, "template", "");
                        Template t = cat.template(id);
                        if (t == null) { err = "不明なテンプレート: " + id; break; }
                        int rot = num(o, "rotation", 0);
                        if (rot >= 4) rot /= 90;
                        rot &= 3;
                        int x = ox + num(o, "x", 0), z = oz + num(o, "z", 0);
                        err = PlacementValidator.checkBox(w, null, x, y, z, x + t.rw(rot) - 1, y + t.h - 1, z + t.rl(rot) - 1);
                        java.util.Map<Block, Block> mat = null;
                        if (err == null && o.has("materials") && o.get("materials").isJsonObject()) {
                            java.util.Map<String, String> raw = new java.util.LinkedHashMap<>();
                            for (java.util.Map.Entry<String, JsonElement> me : o.getAsJsonObject("materials").entrySet()) {
                                if (me.getValue().isJsonPrimitive()) raw.put(me.getKey(), me.getValue().getAsString());
                            }
                            String[] me = new String[1];
                            mat = Materials.parse(raw, me);
                            if (mat == null) err = me[0];
                        }
                        if (err == null) {
                            jobs.add(new TemplateJob(CityActions.ownerOf(p), w, t, new BlockPos(x, y, z), rot,
                                    c.templateFoundation, c.foundationDepth, mat));
                            pr.shapes.add(new int[]{0, x, y, z, x + t.rw(rot) - 1, y + t.h - 1, z + t.rl(rot) - 1});
                            res.buildings++;
                            grow(res, x, z, x + t.rw(rot) - 1, z + t.rl(rot) - 1);
                        }
                        break;
                    }
                    case "road": {
                        JsonArray pts = o.getAsJsonArray("points");
                        int n = pts == null ? 0 : pts.size();
                        if (n < 2 || n > 256) { err = "点の数が不正です"; break; }
                        int[] px = new int[n], pz = new int[n];
                        for (int i = 0; i < n; i++) {
                            JsonElement e = pts.get(i);
                            if (e.isJsonArray()) {
                                px[i] = ox + e.getAsJsonArray().get(0).getAsInt();
                                pz[i] = oz + e.getAsJsonArray().get(1).getAsInt();
                            } else {
                                px[i] = ox + e.getAsJsonObject().get("x").getAsInt();
                                pz[i] = oz + e.getAsJsonObject().get("z").getAsInt();
                            }
                        }
                        int width = num(o, "width", 7);
                        boolean sw = bool(o, "sidewalk", true);
                        Styles.Road style = Styles.road(str(o, "style", "asphalt"));
                        String lines = str(o, "lines", "dashed");
                        if (style == null) { err = "不明な道路スタイル"; break; }
                        if (width < 3 || width > 31) { err = "幅は3〜31にしてください"; break; }
                        if (!List.of("none", "dashed", "solid", "double").contains(lines)) { err = "不明な線スタイル"; break; }
                        int[] b = RoadJob.bounds(px, pz, width, sw);
                        err = PlacementValidator.checkBox(w, null, b[0], y, b[1], b[2], y, b[3]);
                        if (err == null) {
                            jobs.add(new RoadJob(CityActions.ownerOf(p), w, px, pz, width, sw, style, lines, y,
                                    c.roadClearAbove, c.roadFoundation, c.foundationDepth));
                            int r = (width - 1) / 2 + (sw ? 2 : 0);
                            for (int i = 0; i + 1 < n; i++) {
                                pr.shapes.add(new int[]{1, Math.min(px[i], px[i + 1]) - r, y, Math.min(pz[i], pz[i + 1]) - r,
                                        Math.max(px[i], px[i + 1]) + r, y, Math.max(pz[i], pz[i + 1]) + r});
                            }
                            res.roads++;
                            grow(res, b[0], b[1], b[2], b[3]);
                        }
                        break;
                    }
                    case "area": {
                        String style = o.has("block") ? str(o, "block", "") : str(o, "style", "grass");
                        Block blk = Styles.area(style);
                        if (blk == null) { err = "不明な区画スタイル: " + style; break; }
                        int x1 = ox + num(o, "x1", 0), z1 = oz + num(o, "z1", 0), x2 = ox + num(o, "x2", 0), z2 = oz + num(o, "z2", 0);
                        err = PlacementValidator.checkBox(w, null, Math.min(x1, x2), y, Math.min(z1, z2), Math.max(x1, x2), y, Math.max(z1, z2));
                        if (err == null) {
                            jobs.add(new AreaJob(CityActions.ownerOf(p), w, x1, z1, x2, z2, y, blk,
                                    c.roadClearAbove, c.roadFoundation, c.foundationDepth));
                            pr.shapes.add(new int[]{2, Math.min(x1, x2), y, Math.min(z1, z2), Math.max(x1, x2), y, Math.max(z1, z2)});
                            res.areas++;
                            grow(res, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2));
                        }
                        break;
                    }
                    default:
                        err = "不明な種類: " + type;
                }
            } catch (RuntimeException e) {
                err = "項目の形式が不正です (" + e.getClass().getSimpleName() + ")";
            }
            if (err != null) {
                res.skipped++;
                if (res.errors.size() < 8) res.errors.add("#" + idx + " " + type + ": " + err);
            } else {
                res.accepted++;
            }
        }
        long est = 0;
        for (Job j : jobs) est += j.estimate();
        res.estimate = est;
        return pr;
    }

    private static void grow(Result r, int x1, int z1, int x2, int z2) {
        if (r.bounds == null) r.bounds = new int[]{x1, z1, x2, z2};
        else {
            r.bounds[0] = Math.min(r.bounds[0], x1); r.bounds[1] = Math.min(r.bounds[1], z1);
            r.bounds[2] = Math.max(r.bounds[2], x2); r.bounds[3] = Math.max(r.bounds[3], z2);
        }
    }

    /** プレビュー済みプランを実際のジョブとして積む。 */
    public static void submit(Prepared pr) throws IOException {
        if (pr.jobs.isEmpty()) throw new IOException("実行できる項目がありません");
        GroupJob g = new GroupJob(pr.owner, pr.world, "plan:" + pr.name, pr.jobs);
        if (!CityBuilderMod.jobs().canAccept(g.estimate())) throw new IOException("作業キューに入り切りません(規模が大きすぎます)");
        CityBuilderMod.jobs().submit(g);
    }
}
