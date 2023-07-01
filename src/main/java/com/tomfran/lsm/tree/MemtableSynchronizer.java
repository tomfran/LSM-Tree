package com.tomfran.lsm.tree;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemtableSynchronizer {

    LSMTree tree;
    int syncIntervalMills;
    int sizeThreshold;
    ScheduledExecutorService executor;

    public MemtableSynchronizer(LSMTree tree, int syncIntervalMillis, int sizeThreshold) {
        this.tree = tree;
        this.syncIntervalMills = syncIntervalMillis;
        this.sizeThreshold = sizeThreshold;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        executor.scheduleAtFixedRate(this::sync, syncIntervalMills, syncIntervalMills, TimeUnit.MILLISECONDS);
    }

    private void sync() {
        if (tree.memtable.size() > sizeThreshold) {
            System.out.println(">> SYNCING MEMTABLE");
            tree.sync();
            tree.flushLastImmutableMemtable();
        }
    }

    public void stop() {
        executor.shutdown();
    }

}