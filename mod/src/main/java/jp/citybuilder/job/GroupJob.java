package jp.citybuilder.job;

import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.UUID;

/** 複数ジョブを順に実行し、1回の undo で全部戻せるようにする(プラン読み込み用) */
public final class GroupJob extends Job {
    private final List<Job> jobs;
    private int idx;

    public GroupJob(UUID owner, ServerWorld world, String label, List<Job> jobs) {
        super(owner, label, world);
        this.jobs = jobs;
        for (Job j : jobs) j.undo = this.undo;
        if (jobs.isEmpty()) done = true;
    }

    @Override
    public long estimate() {
        long n = 0;
        for (Job j : jobs) n += j.estimate();
        return n;
    }

    @Override
    public int step(int budget) {
        int used = 0;
        while (used < budget && idx < jobs.size()) {
            Job j = jobs.get(idx);
            used += j.step(budget - used);
            if (j.done) { placed += j.placed; idx++; }
        }
        if (idx >= jobs.size()) done = true;
        return used;
    }
}
