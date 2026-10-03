package jp.citybuilder.job;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

/** 道路・区画のスタイル定義 */
public final class Styles {
    public static final class Road {
        public final BlockState surface, sidewalk, white, yellow;
        Road(Block surface, Block sidewalk) {
            this.surface = surface.getDefaultState();
            this.sidewalk = sidewalk.getDefaultState();
            this.white = Blocks.WHITE_CONCRETE.getDefaultState();
            this.yellow = Blocks.YELLOW_CONCRETE.getDefaultState();
        }
    }

    public static final Map<String, Road> ROADS = new LinkedHashMap<>();
    public static final Map<String, Block> AREAS = new LinkedHashMap<>();

    static {
        ROADS.put("asphalt", new Road(Blocks.GRAY_CONCRETE, Blocks.POLISHED_ANDESITE));
        ROADS.put("concrete", new Road(Blocks.LIGHT_GRAY_CONCRETE, Blocks.SMOOTH_STONE));
        ROADS.put("brick", new Road(Blocks.BRICKS, Blocks.STONE_BRICKS));
        ROADS.put("gravel", new Road(Blocks.GRAVEL, Blocks.ANDESITE));

        AREAS.put("grass", Blocks.GRASS_BLOCK);
        AREAS.put("plaza", Blocks.SMOOTH_STONE);
        AREAS.put("asphalt", Blocks.GRAY_CONCRETE);
        AREAS.put("concrete", Blocks.LIGHT_GRAY_CONCRETE);
        AREAS.put("sand", Blocks.SAND);
        AREAS.put("dirt", Blocks.COARSE_DIRT);
        AREAS.put("water", Blocks.WATER);
    }

    public static Road road(String name) {
        return ROADS.get(name == null ? "asphalt" : name.toLowerCase());
    }

    /** 区画スタイル名またはブロックID。無効なら null */
    public static Block area(String name) {
        if (name == null) return null;
        Block b = AREAS.get(name.toLowerCase());
        if (b != null) return b;
        Identifier id = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name);
        if (id == null) return null;
        return Registry.BLOCK.getOrEmpty(id).filter(x -> x != Blocks.AIR).orElse(null);
    }

    public static boolean isRoadBlock(BlockState s) {
        Block b = s.getBlock();
        for (Road r : ROADS.values()) {
            if (b == r.surface.getBlock() || b == r.white.getBlock() || b == r.yellow.getBlock()) return true;
        }
        return false;
    }
}
