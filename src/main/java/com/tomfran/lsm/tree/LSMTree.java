package com.tomfran.lsm.tree;

import com.tomfran.lsm.skiplist.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.sstable.SSTableCompactor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.LinkedList;

public class LSMTree {

    static final int DEFAULT_SYNC_INTERVAL_MILLIS = 2000;
    static final int DEFAULT_SIZE_THRESHOLD = 100;
    private final Object memtableLock = new Object();
    private final Object ssTablesLock = new Object();
    Memtable memtable;
    LinkedList<Memtable> immutableMemtables;
    ObjectArrayList<LinkedList<SSTable>> ssTables;

    int memtableSizeThreshold;

    MemtableSynchronizer memtableSynchronizer;

    public LSMTree() {
        this(true, DEFAULT_SYNC_INTERVAL_MILLIS, DEFAULT_SIZE_THRESHOLD);
    }

    public LSMTree(boolean memtableSync, int syncIntervalMillis, int sizeThreshold) {
        memtable = new Memtable();
        immutableMemtables = new LinkedList<>();
        ssTables = new ObjectArrayList<>();

        if (memtableSync) {
            memtableSynchronizer = new MemtableSynchronizer(this, syncIntervalMillis, sizeThreshold);
            memtableSynchronizer.start();
        }
    }

    public void put(byte[] key, byte[] value) {
        memtable.put(key, value);
    }

    public byte[] get(byte[] key) {
        byte[] value;

        synchronized (memtableLock) {
            if ((value = memtable.get(key)) != null)
                return value;
        }

        synchronized (ssTablesLock) {
            for (Memtable m : immutableMemtables) {
                if ((value = m.get(key)) != null)
                    return value;
            }

            for (LinkedList<SSTable> tables : ssTables) {
                for (SSTable table : tables) {
                    if ((value = table.get(key)) != null)
                        return value;
                }
            }
        }

        return null;
    }

    protected void sync() {
        synchronized (memtableLock) {
            immutableMemtables.addFirst(memtable);
            memtable = new Memtable();
        }
    }

    protected void flushLastImmutableMemtable() {
        Memtable m = immutableMemtables.getLast();
        SSTable table = m.flush();

        synchronized (ssTablesLock) {
            if (ssTables.isEmpty())
                ssTables.add(new LinkedList<>());

            immutableMemtables.removeLast();
            ssTables.get(0).addFirst(table);
        }
    }

    protected void compact(int level) {
        if (level < 0 || level >= ssTables.size())
            throw new RuntimeException("Invalid level");

        LinkedList<SSTable> tables = ssTables.get(level);
        if (tables.size() < 2)
            return;

        SSTableCompactor compactor = new SSTableCompactor(tables);
        SSTable compacted = compactor.compact();

        synchronized (ssTablesLock) {
            ssTables.get(level).clear();
            if (level + 1 >= ssTables.size())
                ssTables.add(new LinkedList<>());
            ssTables.get(level + 1).addFirst(compacted);
        }
    }

    public void stop() {
        if (memtableSynchronizer != null)
            memtableSynchronizer.stop();
    }
}
