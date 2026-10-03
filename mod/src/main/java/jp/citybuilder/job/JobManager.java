package jp.citybuilder.job;

import jp.citybuilder.CityBuilderMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class JobManager {
    private static final class History {
        final ServerWorld world;
        final UndoLog log;
        final String label;
        History(ServerWorld w, UndoLog l, String s) { world = w; log = l; label = s; }
    }

    private final Deque<Job> queue = new ArrayDeque<>();
    private final Map<UUID, Deque<History>> history = new HashMap<>();
    private long pending;

    public synchronized boolean canAccept(long blocks) {
        return pending + blocks <= CityBuilderMod.config().maxPendingBlocks;
    }

    public synchronized void submit(Job j) {
        queue.add(j);
        pending += j.estimate();
    }

    public int queued() { return queue.size(); }
    public long pending() { return pending; }

    public int historySize(UUID owner) {
        Deque<History> h = history.get(owner);
        return h == null ? 0 : h.size();
    }

    /** owner のキュー済みジョブを取り消す(実行途中のものも止まり、そこまでの変更は残る) */
    public synchronized int cancel(UUID owner) {
        int n = 0;
        for (Iterator<Job> it = queue.iterator(); it.hasNext(); ) {
            Job j = it.next();
            if (j.owner.equals(owner)) {
                it.remove();
                pending = Math.max(0, pending - j.estimate());
                if (j.placed > 0) pushHistory(j);
                n++;
            }
        }
        return n;
    }

    /** 直近の履歴を1つ戻すジョブを積む。履歴が無ければ false */
    public synchronized boolean undo(UUID owner) {
        Deque<History> h = history.get(owner);
        if (h == null || h.isEmpty()) return false;
        History e = h.pollLast();
        submit(new RestoreJob(owner, e.world, e.log, "undo:" + e.label));
        return true;
    }

    private void pushHistory(Job j) {
        if (j instanceof RestoreJob || j.undo.size() == 0) return;
        int depth = CityBuilderMod.config().historyDepth;
        if (depth <= 0) return;
        Deque<History> h = history.computeIfAbsent(j.owner, k -> new ArrayDeque<>());
        h.addLast(new History(j.world, j.undo, j.label));
        while (h.size() > depth) h.pollFirst();
    }

    public synchronized void tick(MinecraftServer server) {
        int budget = CityBuilderMod.config().blocksPerTick;
        while (budget > 0 && !queue.isEmpty()) {
            Job j = queue.peek();
            int used = j.step(budget);
            budget -= Math.max(1, used);
            if (j.done) {
                queue.poll();
                pending = Math.max(0, pending - j.estimate());
                pushHistory(j);
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.owner);
                if (p != null) {
                    p.sendMessage(Text.literal("[CityBuilder] 完了: " + j.label + " (" + j.placed + " ブロック変更)"), true);
                }
            }
        }
    }
}
