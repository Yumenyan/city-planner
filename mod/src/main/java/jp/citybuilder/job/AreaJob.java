package jp.citybuilder.job;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/** 矩形区画(公園の芝生・広場・駐車場・水面など)。y が地表ブロックの高さ。 */
public final class AreaJob extends Job {
    private final int x1, z1, x2, z2, y, clearAbove, foundDepth;
    private final boolean foundation;
    private final Block block;
    private int cx, cz;

    public AreaJob(UUID owner, ServerWorld world, int x1, int z1, int x2, int z2, int y, Block block,
                   int clearAbove, boolean foundation, int foundDepth) {
        super(owner, "area", world);
        this.x1 = Math.min(x1, x2); this.x2 = Math.max(x1, x2);
        this.z1 = Math.min(z1, z2); this.z2 = Math.max(z1, z2);
        this.y = y; this.block = block;
        this.clearAbove = clearAbove; this.foundation = foundation; this.foundDepth = foundDepth;
        cx = this.x1; cz = this.z1;
    }

    @Override
    public long estimate() { return (long) (x2 - x1 + 1) * (z2 - z1 + 1); }

    @Override
    public int step(int budget) {
        int used = 0;
        BlockState state = block.getDefaultState();
        boolean water = block == Blocks.WATER;
        while (used < budget) {
            if (cz > z2) { done = true; break; }
            used++;
            used += put(cx, y, cz, state);
            if (water) used += put(cx, y - 1, cz, Blocks.SAND.getDefaultState());
            for (int d = 1; d <= clearAbove; d++) {
                if (!get(cx, y + d, cz).isAir()) used += put(cx, y + d, cz, Blocks.AIR.getDefaultState());
            }
            if (foundation && !water) used += foundation(cx, y, cz, foundDepth, Blocks.STONE.getDefaultState());
            cx++;
            if (cx > x2) { cx = x1; cz++; }
        }
        if (cz > z2) done = true;
        return used;
    }
}
