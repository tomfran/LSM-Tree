package com.tomfran.lsm.tree;

import com.tomfran.lsm.skiplist.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.sstable.SSTableCompactor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.LinkedList;

public class LSMTree {

    /* Default sync and compaction parameters */
    static final int DEFAULT_SYNC_INTERVAL_MILLIS = 2000;
    static final int DEFAULT_SIZE_THRESHOLD = 100;
    static final int DEFAULT_LEVEL_SIZE_THRESHOLD = 10;
    static final int DEFAULT_COMPACTION_INTERVAL_MILLS = 2000;

    /* Active and immutable Memtables */
    private final Object memtableLock = new Object();
    Memtable memtable;
    LinkedList<Memtable> immutableMemtables;
    int memtableSizeLimit;

    /* SSTables */
    private final Object ssTablesLock = new Object();
    ObjectArrayList<LinkedList<SSTable>> ssTables;

    /* Background threads */
    MemtableSynchronizer memtableSynchronizer;
    LevelCompactor levelCompactor;

    public LSMTree() {
        this(true, DEFAULT_SYNC_INTERVAL_MILLIS, DEFAULT_SIZE_THRESHOLD,
                true, DEFAULT_COMPACTION_INTERVAL_MILLS, DEFAULT_LEVEL_SIZE_THRESHOLD);
    }

    public LSMTree(boolean memtableSyncEnabled,
                   int syncIntervalMillis,
                   int inMemorySizeLimit,
                   boolean levelCompactionEnabled,
                   int compactionIntervalMillis,
                   int levelSizeThreshold) {

        memtable = new Memtable();
        memtableSizeLimit = inMemorySizeLimit;
        immutableMemtables = new LinkedList<>();
        ssTables = new ObjectArrayList<>();

        if (memtableSyncEnabled) {
            memtableSynchronizer = new MemtableSynchronizer(this, syncIntervalMillis);
            memtableSynchronizer.start();
        }

        if (levelCompactionEnabled) {
            levelCompactor = new LevelCompactor(this, compactionIntervalMillis, levelSizeThreshold);
            levelCompactor.start();
        }
    }

    public void put(byte[] key, byte[] value) {
        memtable.put(key, value);

        if (memtable.size() >= memtableSizeLimit)
            sync();
    }

    public byte[] get(byte[] key) {
        byte[] value;

        synchronized (memtableLock) {
            if ((value = memtable.get(key)) != null)
                return value;
        }

        synchronized (ssTablesLock) {
            for (Memtable m : immutableMemtables)
                if ((value = m.get(key)) != null)
                    return value;

            for (LinkedList<SSTable> tables : ssTables)
                for (SSTable table : tables)
                    if ((value = table.get(key)) != null)
                        return value;
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
        if (levelCompactor != null)
            levelCompactor.stop();
    }
}
