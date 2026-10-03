package jp.citybuilder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 建材の置き換え(例: white_concrete → light_gray_concrete)。向きなどのプロパティは引き継ぐ。 */
public final class Materials {
    public static final int MAX_ENTRIES = 24;

    private Materials() {}

    public static Optional<Block> block(String s) {
        if (s == null) return Optional.empty();
        s = s.trim().toLowerCase();
        if (s.isEmpty() || s.length() > 80) return Optional.empty();
        Identifier id = Identifier.tryParse(s.contains(":") ? s : "minecraft:" + s);
        return id == null ? Optional.empty() : Registry.BLOCK.getOrEmpty(id);
    }

    /** 置き換え指定(元→先)を検証して Block の対応表にする。エラーは err に入れて null を返す。 */
    public static Map<Block, Block> parse(Map<String, String> raw, String[] err) {
        Map<Block, Block> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        if (raw.size() > MAX_ENTRIES) { err[0] = "建材の置き換えが多すぎます"; return null; }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            Optional<Block> from = block(e.getKey());
            Optional<Block> to = block(e.getValue());
            if (from.isEmpty()) { err[0] = "不明なブロック: " + e.getKey(); return null; }
            if (to.isEmpty()) { err[0] = "不明なブロック: " + e.getValue(); return null; }
            if (to.get().getDefaultState().hasBlockEntity()) { err[0] = "このブロックは使えません: " + e.getValue(); return null; }
            if (from.get() != to.get()) m.put(from.get(), to.get());
        }
        return m;
    }

    public static BlockState replace(BlockState from, Block to) {
        BlockState n = to.getDefaultState();
        for (Property<?> p : from.getProperties()) {
            if (n.contains(p)) n = copy(n, from, p);
        }
        return n;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState n, BlockState from, Property<T> p) {
        return n.with(p, from.get(p));
    }
}
