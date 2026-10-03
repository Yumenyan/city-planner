package jp.citybuilder.job;

import net.minecraft.block.BlockState;

import java.util.Arrays;

/** 変更前のブロックを記録して /citybuilder undo で戻すための履歴 */
public final class UndoLog {
    long[] pos = new long[1024];
    BlockState[] st = new BlockState[1024];
    int n;

    void add(long p, BlockState s) {
        if (n == pos.length) {
            pos = Arrays.copyOf(pos, n * 2);
            st = Arrays.copyOf(st, n * 2);
        }
        pos[n] = p;
        st[n] = s;
        n++;
    }

    public int size() { return n; }
}
