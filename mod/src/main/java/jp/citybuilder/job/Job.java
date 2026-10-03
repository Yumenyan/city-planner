package jp.citybuilder.job;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** tickごとに予算ぶんだけ進める建築ジョブ */
public abstract class Job {
    /** 通知(ネイバー更新なし/形状更新なし)。NOTIFY_LISTENERS(2) | FORCE_STATE(16) */
    protected static final int FLAGS = 2 | 16;

    public final UUID owner;
    public final String label;
    public final ServerWorld world;
    public UndoLog undo = new UndoLog();
    public boolean cancelled;
    public boolean done;
    public long placed;

    private final BlockPos.Mutable mut = new BlockPos.Mutable();

    protected Job(UUID owner, String label, ServerWorld world) {
        this.owner = owner;
        this.label = label;
        this.world = world;
    }

    /** 概算のブロック数(キュー管理用) */
    public abstract long estimate();

    /** budget までの作業量で進め、使った量を返す。完了したら done=true */
    public abstract int step(int budget);

    /** 1ブロック置く(履歴付き)。変化があれば1、なければ0 */
    protected int put(int x, int y, int z, BlockState state) {
        mut.set(x, y, z);
        BlockState old = world.getBlockState(mut);
        if (old == state) return 0;
        undo.add(mut.asLong(), old);
        world.setBlockState(mut, state, FLAGS);
        placed++;
        return 1;
    }

    protected BlockState get(int x, int y, int z) {
        mut.set(x, y, z);
        return world.getBlockState(mut);
    }

    /** 柱の下の空洞(空気・水・草など)を fill で埋める。置いた数を返す */
    protected int foundation(int x, int y, int z, int depth, BlockState fill) {
        int n = 0;
        for (int d = 1; d <= depth; d++) {
            BlockState s = get(x, y - d, z);
            if (!s.getMaterial().isReplaceable()) break;
            n += put(x, y - d, z, fill);
        }
        return n;
    }
}
