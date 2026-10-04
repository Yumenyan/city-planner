package jp.citybuilder.job;

import jp.citybuilder.CityBuilderMod;
import jp.citybuilder.CityConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 建築ジョブの管理。マルチプレイ向けに
 * ・プレイヤーごとのキューを順番に回す(1人の巨大作業が他の人を止めない)
 * ・1tickの処理時間に上限(maxMsPerTick)を設けてサーバーのラグを防ぐ
 * ・ジョブが例外を出してもサーバーを落とさず、そのジョブだけ中止する
 */
public final class JobManager {
    private static final class History {
        final ServerWorld world;
        final UndoLog log;
        final String label;
        History(ServerWorld w, UndoLog l, String s) { world = w; log = l; label = s; }
    }

    private final Map<UUID, Deque<Job>> queues = new LinkedHashMap<>();
    private final Map<UUID, Deque<History>> history = new HashMap<>();
    private long pending;
    private int rr;

    public synchronized int jobsOf(UUID owner) {
        Deque<Job> q = queues.get(owner);
        return q == null ? 0 : q.size();
    }

    /** 受け付けられなければ理由、よければ null */
    public synchronized String check(UUID owner, long blocks) {
        CityConfig c = CityBuilderMod.config();
        if (pending + blocks > c.maxPendingBlocks) return "作業キューが一杯です。完了を待ってください";
        if (jobsOf(owner) >= c.maxJobsPerPlayer) return "あなたの作業が多すぎます(最大 " + c.maxJobsPerPlayer + " 件)。完了を待つか /citybuilder cancel";
        return null;
    }

    public synchronized boolean canAccept(UUID owner, long blocks) { return check(owner, blocks) == null; }

    public synchronized void submit(Job j) {
        queues.computeIfAbsent(j.owner, k -> new ArrayDeque<>()).add(j);
        pending += j.estimate();
    }

    public synchronized int queued() {
        int n = 0;
        for (Deque<Job> q : queues.values()) n += q.size();
        return n;
    }

    public synchronized long pending() { return pending; }

    public int historySize(UUID owner) {
        Deque<History> h = history.get(owner);
        return h == null ? 0 : h.size();
    }

    /** owner のキュー済みジョブを取り消す(実行途中のものも止まり、そこまでの変更は残る) */
    public synchronized int cancel(UUID owner) {
        Deque<Job> q = queues.remove(owner);
        if (q == null) return 0;
        int n = 0;
        for (Iterator<Job> it = q.iterator(); it.hasNext(); ) {
            Job j = it.next();
            pending = Math.max(0, pending - j.estimate());
            if (j.placed > 0) pushHistory(j);
            n++;
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
        CityConfig c = CityBuilderMod.config();
        if (c.historyDepth <= 0) return;
        Deque<History> h = history.computeIfAbsent(j.owner, k -> new ArrayDeque<>());
        h.addLast(new History(j.world, j.undo, j.label));
        long total = 0;
        for (History x : h) total += x.log.size();
        // 回数と、メモリ保護のための合計ブロック数の両方で古い履歴を捨てる
        while (h.size() > c.historyDepth || (h.size() > 1 && total > c.historyMaxBlocks)) {
            total -= h.pollFirst().log.size();
        }
    }

    private void notify(MinecraftServer server, Job j, String msg) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(j.owner);
        if (p != null) p.sendMessage(Text.literal(msg), true);
    }

    public synchronized void tick(MinecraftServer server) {
        if (queues.isEmpty()) return;
        CityConfig c = CityBuilderMod.config();
        int budget = c.blocksPerTick;
        long deadline = System.nanoTime() + Math.max(5, c.maxMsPerTick) * 1_000_000L;
        while (budget > 0 && !queues.isEmpty() && System.nanoTime() < deadline) {
            List<UUID> owners = new ArrayList<>(queues.keySet());
            int n = owners.size();
            int slice = Math.max(256, budget / n);
            for (int k = 0; k < n && budget > 0; k++) {
                UUID o = owners.get((rr + k) % n);
                Deque<Job> q = queues.get(o);
                if (q == null || q.isEmpty()) { queues.remove(o); continue; }
                Job j = q.peek();
                int used = 0;
                try {
                    used = j.step(Math.min(slice, budget));
                } catch (RuntimeException ex) {
                    CityBuilderMod.LOG.error("建築ジョブでエラー: " + j.label, ex);
                    q.poll();
                    pending = Math.max(0, pending - j.estimate());
                    if (j.placed > 0) pushHistory(j);
                    notify(server, j, "[CityBuilder] エラーで中止: " + j.label + " (/citybuilder undo で戻せます)");
                    if (q.isEmpty()) queues.remove(o);
                    continue;
                }
                budget -= Math.max(1, used);
                if (j.done) {
                    q.poll();
                    pending = Math.max(0, pending - j.estimate());
                    pushHistory(j);
                    notify(server, j, "[CityBuilder] 完了: " + j.label + " (" + j.placed + " ブロック変更)");
                    if (q.isEmpty()) queues.remove(o);
                }
                if (System.nanoTime() >= deadline) break;
            }
            rr++;
        }
    }
}
