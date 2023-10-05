package com.tomfran.lsm.tree;

import com.tomfran.lsm.memtable.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.ByteArrayPair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class LSMTree {

    static final int DEFAULT_MEMTABLE_MAX_SIZE = 1 << 15;
    static final int DEFAULT_SSTABLE_SAMPLE_SIZE = 1 << 10;
    static final String DEFAULT_DATA_DIRECTORY = "LSM-data";

    final Object mutableMemtableLock = new Object();
    final Object immutableMemtablesLock = new Object();
    final Object tableLock = new Object();

    Memtable mutableMemtable;
    final int mutableMemtableMaxSize;
    final String dataDir;

    LinkedList<Memtable> immutableMemtables;
    SSTable table;
    ExecutorService memtableFlusher;

    /**
     * Creates a new LSMTree with a default memtable size and data directory.
     */
    public LSMTree() {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_DATA_DIRECTORY);
    }

    /**
     * Creates a new LSMTree with a memtable size and data directory.
     *
     * @param memtableMaxSize The maximum size of the memtable before it is flushed to disk.
     * @param dataDir         The directory to store the data in.
     */
    public LSMTree(int memtableMaxSize, String dataDir) {
        mutableMemtableMaxSize = memtableMaxSize;
        this.dataDir = dataDir;
        createDataDir();

        mutableMemtable = new Memtable(memtableMaxSize);
        immutableMemtables = new LinkedList<>();
        memtableFlusher = newSingleThreadExecutor();
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
            if ((result = table.get(key)) != null)
                return result;
        }

        return null;
    }

    public void stop() {
        memtableFlusher.shutdownNow();
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

        String filename = String.format("%s/sst_%d", dataDir, System.currentTimeMillis());

        synchronized (tableLock) {
            if (table == null)
                table = mutableMemtable.flush(filename, DEFAULT_SSTABLE_SAMPLE_SIZE);
            else {
                var newTable = SSTable.merge(filename, DEFAULT_SSTABLE_SAMPLE_SIZE, memtableToFlush, table);
                table.deleteFiles();
                table = newTable;
            }
        }

        // remove flushed memtable from immutable memtables
        synchronized (immutableMemtablesLock) {
            immutableMemtables.removeLast();
        }
    }

    private void createDataDir() {
        try {
            Files.createDirectory(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("Could not create data directory", e);
        }
    }

}
