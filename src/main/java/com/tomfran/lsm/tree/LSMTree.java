package com.tomfran.lsm.tree;

import com.tomfran.lsm.memtable.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class LSMTree {

    static final int DEFAULT_MEMTABLE_MAX_SIZE = 1 << 10;
    static final int DEFAULT_TABLE_LEVEL_MAX_SIZE = 5;
    static final int DEFAULT_SSTABLE_SAMPLE_SIZE = 1 << 10;
    static final String DEFAULT_DATA_DIRECTORY = "LSM-data";

    final Object mutableMemtableLock = new Object();
    final Object immutableMemtablesLock = new Object();
    final Object tableLock = new Object();

    final int mutableMemtableMaxSize;
    final int tableLevelMaxSize;
    final String dataDir;

    Memtable mutableMemtable;
    LinkedList<Memtable> immutableMemtables;
    ObjectArrayList<LinkedList<SSTable>> tables;
    ExecutorService memtableFlusher;
    ExecutorService tableCompactor;

    /**
     * Creates a new LSMTree with a default memtable size and data directory.
     */
    public LSMTree() {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_TABLE_LEVEL_MAX_SIZE, DEFAULT_DATA_DIRECTORY);
    }

    /**
     * Creates a new LSMTree with a memtable size and data directory.
     *
     * @param memtableMaxSize The maximum size of the memtable before it is flushed to disk.
     * @param dataDir         The directory to store the data in.
     */
    public LSMTree(int mutableMemtableMaxSize, int tableLevelMaxSize, String dataDir) {
        this.mutableMemtableMaxSize = mutableMemtableMaxSize;
        this.tableLevelMaxSize = tableLevelMaxSize;
        this.dataDir = dataDir;
        createDataDir();

        mutableMemtable = new Memtable(mutableMemtableMaxSize);
        immutableMemtables = new LinkedList<>();
        tables = new ObjectArrayList<>();
        tables.add(new LinkedList<>());

        memtableFlusher = newSingleThreadExecutor();
        tableCompactor = newSingleThreadExecutor();
    }


    /**
     * Adds an item to the LSMTree.
     * If the memtable is full, it is flushed to disk.
     *
     * @param item The item to add.
     */
    public void add(ByteArrayPair item) {
        synchronized (mutableMemtableLock) {
            mutableMemtable.add(item);
            checkMemtableSize();
        }
    }

    /**
     * Removes an item from the LSMTree.
     * This is done by adding a tombstone to the memtable.
     *
     * @param key The key of the item to remove.
     */
    public void delete(byte[] key) {
        synchronized (mutableMemtableLock) {
            mutableMemtable.remove(key);
            checkMemtableSize();
        }
    }

    /**
     * Gets an item from the LSMTree.
     *
     * @param key The key of the item to get.
     * @return The value of the item, or null if it does not exist.
     */
    public byte[] get(byte[] key) {
        byte[] result;

        synchronized (mutableMemtableLock) {
            if ((result = mutableMemtable.get(key)) != null)
                return result;
        }

        synchronized (immutableMemtablesLock) {
            for (Memtable memtable : immutableMemtables)
                if ((result = memtable.get(key)) != null)
                    return result;
        }

        synchronized (tableLock) {
            for (LinkedList<SSTable> level : tables)
                for (SSTable table : level)
                    if ((result = table.get(key)) != null)
                        return result;
        }

        return null;
    }

    public void stop() {
        memtableFlusher.shutdownNow();
        tableCompactor.shutdownNow();
    }

    private void checkMemtableSize() {
        if (mutableMemtable.size() <= mutableMemtableMaxSize)
            return;

        synchronized (immutableMemtablesLock) {
            immutableMemtables.addFirst(mutableMemtable);
            mutableMemtable = new Memtable(mutableMemtableMaxSize);
            memtableFlusher.execute(this::flushLastMemtable);
        }
    }

    private void flushLastMemtable() {
        Memtable memtableToFlush;

        // extract immutable memtable which need to be flushed
        synchronized (immutableMemtablesLock) {
            if (immutableMemtables.isEmpty())
                return;

            memtableToFlush = immutableMemtables.getLast();
        }

        String filename = getTableName();

        synchronized (tableLock) {
            tables.get(0).addFirst(new SSTable(filename, memtableToFlush.iterator(), DEFAULT_SSTABLE_SAMPLE_SIZE));
            tableCompactor.execute(this::compactTables);
        }

        // remove flushed memtable from immutable memtables
        synchronized (immutableMemtablesLock) {
            immutableMemtables.removeLast();
        }
    }

    private void compactTables() {
        synchronized (tableLock) {

            int n = tables.size();

            for (int i = 0; i < n; i++) {
                var level = tables.get(i);
                if (level.size() <= tableLevelMaxSize)
                    continue;

                var table = SSTable.merge(getTableName(), DEFAULT_SSTABLE_SAMPLE_SIZE, level);

                if (i == n - 1)
                    tables.add(new LinkedList<>());

                tables.get(i + 1).addFirst(table);
                level.forEach(SSTable::closeAndDelete);
                level.clear();
            }
        }
    }

    private String getTableName() {
        return String.format("%s/sst_%d", dataDir, System.currentTimeMillis());
    }

    private void createDataDir() {
        try {
            Files.createDirectory(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("Could not create data directory", e);
        }
    }

}
