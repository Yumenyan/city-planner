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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    public static Template load(String id, Path file) throws IOException {
        NbtCompound root;
        try (InputStream in = Files.newInputStream(file)) {
            root = NbtIo.readCompressed(in);
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
        int max = -1;
        for (String k : pc.getKeys()) max = Math.max(max, pc.getInt(k));
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
