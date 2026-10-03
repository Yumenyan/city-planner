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
        public final List<String> errors = new ArrayList<>();
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

    public static Result load(ServerWorld w, ServerPlayerEntity p, String name, BlockPos origin) throws IOException {
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

        Result res = new Result();
        List<Job> jobs = new ArrayList<>();
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
                        if (err == null) jobs.add(new TemplateJob(CityActions.ownerOf(p), w, t, new BlockPos(x, y, z), rot,
                                c.templateFoundation, c.foundationDepth));
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
                        if (width < 1 || width > 31) { err = "幅が不正です"; break; }
                        if (!List.of("none", "dashed", "solid", "double").contains(lines)) { err = "不明な線スタイル"; break; }
                        int[] b = RoadJob.bounds(px, pz, width, sw);
                        err = PlacementValidator.checkBox(w, null, b[0], y, b[1], b[2], y, b[3]);
                        if (err == null) jobs.add(new RoadJob(CityActions.ownerOf(p), w, px, pz, width, sw, style, lines, y,
                                c.roadClearAbove, c.roadFoundation, c.foundationDepth));
                        break;
                    }
                    case "area": {
                        String style = o.has("block") ? str(o, "block", "") : str(o, "style", "grass");
                        Block blk = Styles.area(style);
                        if (blk == null) { err = "不明な区画スタイル: " + style; break; }
                        int x1 = ox + num(o, "x1", 0), z1 = oz + num(o, "z1", 0), x2 = ox + num(o, "x2", 0), z2 = oz + num(o, "z2", 0);
                        err = PlacementValidator.checkBox(w, null, Math.min(x1, x2), y, Math.min(z1, z2), Math.max(x1, x2), y, Math.max(z1, z2));
                        if (err == null) jobs.add(new AreaJob(CityActions.ownerOf(p), w, x1, z1, x2, z2, y, blk,
                                c.roadClearAbove, c.roadFoundation, c.foundationDepth));
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
        if (!jobs.isEmpty()) {
            GroupJob g = new GroupJob(CityActions.ownerOf(p), w, "plan:" + name, jobs);
            res.estimate = g.estimate();
            if (!CityBuilderMod.jobs().canAccept(res.estimate)) throw new IOException("作業キューに入り切りません(規模が大きすぎます)");
            CityBuilderMod.jobs().submit(g);
        }
        return res;
    }
}
