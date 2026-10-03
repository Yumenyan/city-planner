package jp.citybuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * config/citybuilder/catalog.json と templates/*.schem を読み込む。
 * catalog.json に無い .schem も「未分類」として自動で追加する(ファイルを置くだけで増やせる)。
 */
public final class BuildingCatalog {
    private static final Pattern ID_RE = Pattern.compile("[A-Za-z0-9_\\-]+");

    public static final class Entry {
        public String id;
        public String name;
        public String category;
        public String description;
        public int floors;
        public int[] size;
    }

    private static final class CatalogFile {
        List<Entry> buildings;
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, Template> templates = new LinkedHashMap<>();
    /** アップロードされた建物のメタ情報(uploads.json に保存) */
    private final List<Entry> uploaded = new ArrayList<>();

    public static BuildingCatalog load() {
        BuildingCatalog c = new BuildingCatalog();
        Path dir = CityConfig.dir();
        Path tdir = dir.resolve("templates");
        try {
            Files.createDirectories(tdir);
            Files.createDirectories(dir.resolve("plans"));
        } catch (IOException ignored) {}

        List<Entry> listed = new ArrayList<>();
        Path cf = dir.resolve("catalog.json");
        if (Files.exists(cf)) {
            try (Reader r = Files.newBufferedReader(cf, StandardCharsets.UTF_8)) {
                CatalogFile f = new Gson().fromJson(r, CatalogFile.class);
                if (f != null && f.buildings != null) listed = f.buildings;
            } catch (Exception e) {
                CityBuilderMod.LOG.warn("catalog.json を読めませんでした: {}", e.toString());
            }
        }
        Path uf = dir.resolve("uploads.json");
        if (Files.exists(uf)) {
            try (Reader r = Files.newBufferedReader(uf, StandardCharsets.UTF_8)) {
                CatalogFile f = new Gson().fromJson(r, CatalogFile.class);
                if (f != null && f.buildings != null) {
                    for (Entry e : f.buildings) {
                        if (e == null || e.id == null) continue;
                        c.uploaded.add(e);
                        listed.removeIf(x -> x != null && e.id.equals(x.id));
                        listed.add(e);
                    }
                }
            } catch (Exception e) {
                CityBuilderMod.LOG.warn("uploads.json を読めませんでした: {}", e.toString());
            }
        }
        for (Entry e : listed) {
            if (e == null || e.id == null || !ID_RE.matcher(e.id).matches()) continue;
            if (!c.tryLoadTemplate(e.id, tdir)) continue;
            if (e.name == null || e.name.isEmpty()) e.name = e.id;
            if (e.category == null || e.category.isEmpty()) e.category = "未分類";
            if (e.description == null) e.description = "";
            c.entries.put(e.id, e);
        }
        try (Stream<Path> s = Files.list(tdir)) {
            List<Path> files = new ArrayList<>();
            s.filter(p -> p.getFileName().toString().endsWith(".schem")).sorted().forEach(files::add);
            for (Path p : files) {
                String fn = p.getFileName().toString();
                String id = fn.substring(0, fn.length() - ".schem".length());
                if (c.entries.containsKey(id) || !ID_RE.matcher(id).matches()) continue;
                if (!c.tryLoadTemplate(id, tdir)) continue;
                Entry e = new Entry();
                e.id = id; e.name = id; e.category = "未分類"; e.description = "";
                c.entries.put(id, e);
            }
        } catch (IOException e) {
            CityBuilderMod.LOG.warn("templates フォルダを読めませんでした: {}", e.toString());
        }
        // サイズは実ファイルの値を正とする
        for (Entry e : c.entries.values()) {
            Template t = c.templates.get(e.id);
            e.size = new int[]{t.w, t.h, t.l};
        }
        CityBuilderMod.LOG.info("建物カタログ: {} 件", c.entries.size());
        return c;
    }

    private boolean tryLoadTemplate(String id, Path tdir) {
        Path p = tdir.resolve(id + ".schem");
        if (!Files.exists(p)) {
            CityBuilderMod.LOG.warn("テンプレートファイルがありません: {}", p);
            return false;
        }
        try {
            templates.put(id, Template.load(id, p));
            return true;
        } catch (Exception e) {
            CityBuilderMod.LOG.warn("テンプレート {} を読めませんでした: {}", id, e.toString());
            return false;
        }
    }

    public static boolean validId(String id) {
        return id != null && id.length() <= 48 && ID_RE.matcher(id).matches();
    }

    public boolean has(String id) { return entries.containsKey(id); }

    /** アップロードされたテンプレートを保存して登録する。 */
    public synchronized void addUpload(Entry e, Template t, byte[] bytes) throws IOException {
        Path tdir = CityConfig.dir().resolve("templates");
        Files.createDirectories(tdir);
        Files.write(tdir.resolve(e.id + ".schem"), bytes);
        e.size = new int[]{t.w, t.h, t.l};
        templates.put(e.id, t);
        entries.put(e.id, e);
        uploaded.removeIf(x -> e.id.equals(x.id));
        uploaded.add(e);
        CatalogFile f = new CatalogFile();
        f.buildings = uploaded;
        try (Writer w = Files.newBufferedWriter(CityConfig.dir().resolve("uploads.json"), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(f, w);
        }
    }

    public Entry get(String id) { return id == null ? null : entries.get(id); }
    public Template template(String id) { return id == null ? null : templates.get(id); }
    public Iterable<Entry> all() { return entries.values(); }
    public int size() { return entries.size(); }

    /** クライアントに送る内容(ブロックデータは含まない) */
    public String toClientJson(boolean canBuild, int maxDistance, boolean canUpload, int maxUploadBytes) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);
        root.addProperty("canBuild", canBuild);
        root.addProperty("maxDistance", maxDistance);
        root.addProperty("canUpload", canUpload);
        root.addProperty("maxUploadBytes", maxUploadBytes);
        JsonArray arr = new JsonArray();
        for (Entry e : entries.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("name", e.name);
            o.addProperty("category", e.category);
            o.addProperty("description", e.description);
            o.addProperty("floors", e.floors);
            JsonArray sz = new JsonArray();
            sz.add(e.size[0]); sz.add(e.size[1]); sz.add(e.size[2]);
            o.add("size", sz);
            JsonArray mats = new JsonArray();
            Template t = templates.get(e.id);
            if (t != null) {
                for (Map.Entry<String, Integer> m : t.materials(16)) {
                    JsonObject mo = new JsonObject();
                    mo.addProperty("b", m.getKey());
                    mo.addProperty("n", m.getValue());
                    mats.add(mo);
                }
            }
            o.add("materials", mats);
            arr.add(o);
        }
        root.add("buildings", arr);
        return root.toString();
    }
}
