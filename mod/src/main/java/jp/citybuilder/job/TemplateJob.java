package jp.citybuilder.job;

import jp.citybuilder.Template;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;

/** 建物(.schem)を配置するジョブ。pos は回転後の最小コーナー。 */
public final class TemplateJob extends Job {
    private final Template t;
    private final BlockPos o;
    private final int rot;
    private final BlockState[] pal;
    private final boolean foundation;
    private final int foundDepth;
    private final int rw, rl;
    private final int total;
    private int fc;   // 基礎の列カウンタ
    private int idx;  // ブロックカウンタ
    private final int[] tmp = new int[2];

    public TemplateJob(UUID owner, ServerWorld world, Template t, BlockPos origin, int rot,
                       boolean foundation, int foundDepth) {
        this(owner, world, t, origin, rot, foundation, foundDepth, null);
    }

    public TemplateJob(UUID owner, ServerWorld world, Template t, BlockPos origin, int rot,
                       boolean foundation, int foundDepth, Map<Block, Block> materials) {
        super(owner, t.id, world);
        this.t = t;
        this.o = origin;
        this.rot = rot & 3;
        this.pal = t.palette(this.rot, materials);
        this.foundation = foundation;
        this.foundDepth = foundDepth;
        this.rw = t.rw(this.rot);
        this.rl = t.rl(this.rot);
        this.total = t.w * t.h * t.l;
    }

    @Override
    public long estimate() { return (long) total + (foundation ? (long) rw * rl : 0); }

    @Override
    public int step(int budget) {
        int used = 0;
        if (foundation) {
            int cols = rw * rl;
            while (fc < cols && used < budget) {
                int cx = fc % rw, cz = fc / rw;
                used += 1 + foundation(o.getX() + cx, o.getY(), o.getZ() + cz, foundDepth, Blocks.STONE.getDefaultState());
                fc++;
            }
            if (fc < cols) return used;
        }
        while (idx < total && used < budget) {
            int x = idx % t.w;
            int z = (idx / t.w) % t.l;
            int y = idx / (t.w * t.l);
            t.map(x, z, rot, tmp);
            put(o.getX() + tmp[0], o.getY() + y, o.getZ() + tmp[1], pal[t.paletteIndexAt(idx)]);
            idx++;
            used++;
        }
        if (idx >= total) done = true;
        return used;
    }
}
