package jp.citybuilder.job;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** 履歴を逆順に適用して元に戻す(これ自体は履歴に残さない) */
public final class RestoreJob extends Job {
    private final UndoLog log;
    private int i;

    public RestoreJob(UUID owner, ServerWorld world, UndoLog log, String label) {
        super(owner, label, world);
        this.log = log;
        this.i = log.n - 1;
    }

    @Override
    public long estimate() { return log.n; }

    @Override
    public int step(int budget) {
        int used = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();
        while (i >= 0 && used < budget) {
            m.set(log.pos[i]);
            world.setBlockState(m, log.st[i], FLAGS);
            i--;
            used++;
        }
        if (i < 0) done = true;
        return used;
    }
}
