package jp.citybuilder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Sponge Schematic (v2 / v3) の読み込み。
 * 原点は最小コーナー。回転0では正面が南(+Z)。
 * データ順は y→z→x。
 */
public final class Template {
    public final String id;
    public final int w, h, l;
    private final BlockState[] palette;
    private final int[] data;
    private final BlockState[][] rotCache = new BlockState[4][];

    private Template(String id, int w, int h, int l, BlockState[] palette, int[] data) {
        this.id = id; this.w = w; this.h = h; this.l = l; this.palette = palette; this.data = data;
    }

    public long volume() { return (long) w * h * l; }
    public int rw(int rot) { return (rot & 1) == 0 ? w : l; }
    public int rl(int rot) { return (rot & 1) == 0 ? l : w; }

    /** 回転後のパレット */
    public BlockState[] palette(int rot) {
        rot &= 3;
        BlockState[] r = rotCache[rot];
        if (r == null) {
            r = new BlockState[palette.length];
            BlockRotation br = rot == 0 ? BlockRotation.NONE
                    : rot == 1 ? BlockRotation.CLOCKWISE_90
                    : rot == 2 ? BlockRotation.CLOCKWISE_180
                    : BlockRotation.COUNTERCLOCKWISE_90;
            for (int i = 0; i < r.length; i++) r[i] = palette[i].rotate(br);
            rotCache[rot] = r;
        }
        return r;
    }

    /** 建材を置き換えた回転後パレット。map が空なら通常のパレット。 */
    public BlockState[] palette(int rot, Map<Block, Block> map) {
        BlockState[] base = palette(rot);
        if (map == null || map.isEmpty()) return base;
        BlockState[] r = new BlockState[base.length];
        for (int i = 0; i < r.length; i++) {
            Block to = map.get(base[i].getBlock());
            r[i] = to == null ? base[i] : Materials.replace(base[i], to);
        }
        return r;
    }

    private List<Map.Entry<String, Integer>> materialCache;

    /** 使われているブロックの種類と個数(多い順、空気を除く)。最大 limit 件。 */
    public synchronized List<Map.Entry<String, Integer>> materials(int limit) {
        if (materialCache == null) {
            int[] cnt = new int[palette.length];
            for (int v : data) cnt[v]++;
            Map<String, Integer> m = new HashMap<>();
            for (int i = 0; i < palette.length; i++) {
                if (cnt[i] == 0 || palette[i].isAir()) continue;
                String k = Registry.BLOCK.getId(palette[i].getBlock()).toString();
                m.merge(k, cnt[i], Integer::sum);
            }
            List<Map.Entry<String, Integer>> l = new ArrayList<>(m.entrySet());
            l.sort((a, b) -> b.getValue() != a.getValue().intValue() ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()));
            materialCache = l;
        }
        return materialCache.size() <= limit ? materialCache : materialCache.subList(0, limit);
    }

    public int paletteIndexAt(int idx) { return data[idx]; }

    /** 元座標(x,z)を回転後の相対座標へ(時計回り90度×rot)。結果は {nx, nz}。 */
    public void map(int x, int z, int rot, int[] out) {
        switch (rot & 3) {
            case 0: out[0] = x; out[1] = z; break;
            case 1: out[0] = l - 1 - z; out[1] = x; break;
            case 2: out[0] = w - 1 - x; out[1] = l - 1 - z; break;
            default: out[0] = z; out[1] = w - 1 - x; break;
        }
    }

    /** 展開後サイズの上限(zip爆弾対策) */
    private static final int MAX_UNZIPPED = 64 * 1024 * 1024;

    public static Template load(String id, Path file) throws IOException {
        if (Files.size(file) > 32L * 1024 * 1024) throw new IOException("ファイルが大きすぎます");
        return load(id, Files.readAllBytes(file));
    }

    /** gzip圧縮された .schem のバイト列から読み込む。展開サイズを制限する。 */
    public static Template load(String id, byte[] gz) throws IOException {
        NbtCompound root;
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                if (bo.size() + n > MAX_UNZIPPED) throw new IOException("展開後のサイズが大きすぎます");
                bo.write(buf, 0, n);
            }
            root = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray())));
        }
        if (root.contains("Schematic", 10)) root = root.getCompound("Schematic");
        int w = root.getShort("Width") & 0xFFFF;
        int h = root.getShort("Height") & 0xFFFF;
        int l = root.getShort("Length") & 0xFFFF;
        NbtCompound pc;
        byte[] bytes;
        if (root.contains("Blocks", 10)) { // v3
            NbtCompound b = root.getCompound("Blocks");
            pc = b.getCompound("Palette");
            bytes = b.getByteArray("Data");
        } else { // v2
            pc = root.getCompound("Palette");
            bytes = root.getByteArray("BlockData");
        }
        if (w <= 0 || h <= 0 || l <= 0) throw new IOException("サイズが不正です");
        if (w > 512 || h > 384 || l > 512) throw new IOException("サイズが大きすぎます");
        int max = -1;
        for (String k : pc.getKeys()) max = Math.max(max, pc.getInt(k));
        if (max > 65535) throw new IOException("パレットが大きすぎます");
        BlockState[] palette = new BlockState[max + 1];
        for (String k : pc.getKeys()) palette[pc.getInt(k)] = parseState(k);
        for (int i = 0; i < palette.length; i++) if (palette[i] == null) palette[i] = Blocks.AIR.getDefaultState();

        long total = (long) w * h * l;
        if (total > 50_000_000L) throw new IOException("テンプレートが大きすぎます");
        int[] data = new int[(int) total];
        int p = 0;
        for (int i = 0; i < data.length; i++) {
            int value = 0, shift = 0;
            while (true) {
                if (p >= bytes.length) throw new IOException("BlockData が途中で終わっています");
                int b = bytes[p++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
                if (shift > 28) throw new IOException("varint が不正です");
            }
            if (value >= palette.length) value = 0;
            data[i] = value;
        }
        return new Template(id, w, h, l, palette, data);
    }

    /** "minecraft:oak_stairs[facing=east,half=bottom]" 形式を BlockState にする。未知のブロックは空気。 */
    public static BlockState parseState(String s) {
        String name = s;
        String props = null;
        int i = s.indexOf('[');
        if (i >= 0) {
            name = s.substring(0, i);
            int j = s.lastIndexOf(']');
            props = s.substring(i + 1, j > i ? j : s.length());
        }
        Identifier rid = Identifier.tryParse(name);
        Optional<Block> ob = rid == null ? Optional.empty() : Registry.BLOCK.getOrEmpty(rid);
        if (ob.isEmpty()) {
            CityBuilderMod.LOG.warn("未知のブロック: {}", s);
            return Blocks.AIR.getDefaultState();
        }
        BlockState st = ob.get().getDefaultState();
        if (props != null && !props.isEmpty()) {
            for (String kv : props.split(",")) {
                int eq = kv.indexOf('=');
                if (eq <= 0) continue;
                Property<?> prop = ob.get().getStateManager().getProperty(kv.substring(0, eq).trim());
                if (prop != null) st = withProp(st, prop, kv.substring(eq + 1).trim());
            }
        }
        return st;
    }

    private static <T extends Comparable<T>> BlockState withProp(BlockState st, Property<T> prop, String v) {
        Optional<T> o = prop.parse(v);
        return o.isPresent() ? st.with(prop, o.get()) : st;
    }
}
