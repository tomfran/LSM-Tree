package com.tomfran.lsm.tree;

public class MemtableSynchronizer extends BackgroundExecutor {

    LSMTree tree;

    public MemtableSynchronizer(LSMTree tree, int syncIntervalMillis) {
        super(syncIntervalMillis);
        this.tree = tree;
        setRunnable(this::sync);
    }

    private void sync() {
        if (!tree.immutableMemtables.isEmpty()) {
            System.out.println(">> Syncing oldest immutable memtable");
            tree.flushLastImmutableMemtable();
        }
    }
}