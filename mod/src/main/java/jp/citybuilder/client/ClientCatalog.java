package jp.citybuilder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** サーバーから受け取った建物カタログ(ブロックデータは持たない) */
public final class ClientCatalog {
    public static final class Entry {
        public String id, name, category, description, author;
        public int floors, w, h, l;
        /** 使われている建材 {ブロックID, 個数}(多い順) */
        public final List<Object[]> materials = new ArrayList<>();
    }

    private static List<Entry> entries = new ArrayList<>();
    private static List<String> categories = new ArrayList<>();
    public static boolean canBuild = false;
    public static int maxDistance = 160;
    public static boolean canUpload = false;
    public static List<String> plans = new ArrayList<>();
    public static int maxUploadBytes = 2_000_000;

    private ClientCatalog() {}

    public static void update(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            canBuild = root.has("canBuild") && root.get("canBuild").getAsBoolean();
            maxDistance = root.has("maxDistance") ? root.get("maxDistance").getAsInt() : 160;
            canUpload = root.has("canUpload") && root.get("canUpload").getAsBoolean();
            maxUploadBytes = root.has("maxUploadBytes") ? root.get("maxUploadBytes").getAsInt() : 2_000_000;
            List<String> pl = new ArrayList<>();
            if (root.has("plans")) for (JsonElement pe : root.getAsJsonArray("plans")) pl.add(pe.getAsString());
            plans = pl;
            List<Entry> list = new ArrayList<>();
            Set<String> cats = new LinkedHashSet<>();
            JsonArray arr = root.getAsJsonArray("buildings");
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                Entry e = new Entry();
                e.id = o.get("id").getAsString();
                e.name = o.get("name").getAsString();
                e.category = o.get("category").getAsString();
                e.description = o.has("description") ? o.get("description").getAsString() : "";
                e.author = o.has("author") ? o.get("author").getAsString() : null;
                e.floors = o.has("floors") ? o.get("floors").getAsInt() : 0;
                JsonArray sz = o.getAsJsonArray("size");
                e.w = sz.get(0).getAsInt();
                e.h = sz.get(1).getAsInt();
                e.l = sz.get(2).getAsInt();
                if (o.has("materials")) {
                    for (JsonElement me : o.getAsJsonArray("materials")) {
                        JsonObject mo = me.getAsJsonObject();
                        e.materials.add(new Object[]{mo.get("b").getAsString(), mo.get("n").getAsInt()});
                    }
                }
                list.add(e);
                cats.add(e.category);
            }
            entries = list;
            categories = new ArrayList<>(cats);
        } catch (RuntimeException ex) {
            // 不正なデータは無視(既存のカタログを維持)
        }
    }

    public static List<Entry> all() { return entries; }
    public static List<String> categories() { return categories; }

    public static List<Entry> inCategory(String c) {
        List<Entry> r = new ArrayList<>();
        for (Entry e : entries) if (e.category.equals(c)) r.add(e);
        return r;
    }
}
