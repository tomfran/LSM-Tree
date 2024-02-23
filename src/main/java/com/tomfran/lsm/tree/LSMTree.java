package com.tomfran.lsm.tree;

import com.tomfran.lsm.memtable.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.management.relation.RoleUnresolvedList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * LSM Tree implementation.
 * <p>
 * Writes are added to the Memtable, which is flushed when a certain size is reached.
 * SSTables are divided in levels, each level storing bigger tables.
 * <p>
 * When flushed, a Memtable becomes an SSTable at level 1, when the level exceeds
 * a threshold, all its tables are merged and added to the next level.
 * <p>
 * Background executors take care of flushing and compaction.
 */
public class LSMTree {

    static final long DEFAULT_MEMTABLE_MAX_BYTE_SIZE = 1024 * 1024 * 32;
    static final int DEFAULT_LEVEL_ZERO_MAX_SIZE = 2;
    static final double LEVEL_INCR_FACTOR = 1.75;

    static final String DEFAULT_DATA_DIRECTORY = "LSM-data";

    final Object mutableMemtableLock = new Object();
    final Object immutableMemtablesLock = new Object();
    final Object tableLock = new Object();

    final long mutableMemtableMaxSize;
    final int maxLevelZeroSstNumber;
    final long maxLevelZeroSstByteSize;
    final String dataDir;

    Memtable mutableMemtable;
    LinkedList<Memtable> immutableMemtables;
    ObjectArrayList<ObjectArrayList<SSTable>> levels;

    ScheduledExecutorService memtableFlusher;
    ScheduledExecutorService tableCompactor;

    /**
     * Creates a new LSMTree with a default memtable size and data directory.
     */
    public LSMTree() {
        this(DEFAULT_MEMTABLE_MAX_BYTE_SIZE, DEFAULT_LEVEL_ZERO_MAX_SIZE, DEFAULT_DATA_DIRECTORY);
    }

    /**
     * Creates a new LSMTree with a memtable size and data directory.
     *
     * @param mutableMemtableMaxByteSize The maximum size of the memtable before it is flushed to disk.
     * @param dataDir                    The directory to store the data in.
     */
    public LSMTree(long mutableMemtableMaxByteSize, int maxLevelZeroSstNumber, String dataDir) {
        this.mutableMemtableMaxSize = mutableMemtableMaxByteSize;
        this.maxLevelZeroSstNumber = maxLevelZeroSstNumber;
        this.maxLevelZeroSstByteSize = mutableMemtableMaxByteSize * 2;
        this.dataDir = dataDir;
        createDataDir();

        mutableMemtable = new Memtable();
        immutableMemtables = new LinkedList<>();
        levels = new ObjectArrayList<>();
        levels.add(new ObjectArrayList<>());

        memtableFlusher = newSingleThreadScheduledExecutor();
        memtableFlusher.scheduleAtFixedRate(this::flushMemtable, 50, 50, TimeUnit.MILLISECONDS);

        tableCompactor = newSingleThreadScheduledExecutor();
        tableCompactor.scheduleAtFixedRate(this::levelCompaction, 200, 200, TimeUnit.MILLISECONDS);
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
            for (ObjectArrayList<SSTable> level : levels)
                for (SSTable table : level)
                    if ((result = table.get(key)) != null)
                        return result;
        }

        return null;
    }

    /**
     * Stop the background threads.
     */
    public void stop() {
        memtableFlusher.shutdownNow();
        tableCompactor.shutdownNow();
    }

    private void checkMemtableSize() {
        if (mutableMemtable.byteSize() <= mutableMemtableMaxSize)
            return;

        synchronized (immutableMemtablesLock) {
            immutableMemtables.addFirst(mutableMemtable);
            mutableMemtable = new Memtable();
        }
    }

    private void flushMemtable() {
        Memtable memtableToFlush;
        synchronized (immutableMemtablesLock) {
            if (immutableMemtables.isEmpty())
                return;

            memtableToFlush = immutableMemtables.getLast();
        }

        SSTable table = new SSTable(dataDir, memtableToFlush.iterator(), mutableMemtableMaxSize * 2);

        synchronized (tableLock) {
            levels.get(0).add(0, table);
        }

        synchronized (immutableMemtablesLock) {
            immutableMemtables.removeLast();
        }
    }

    private void levelCompaction() {
        synchronized (tableLock) {
            int n = levels.size();

            int maxLevelSize = maxLevelZeroSstNumber;
            long sstMaxSize = maxLevelZeroSstByteSize;

            for (int i = 0; i < n; i++) {
                ObjectArrayList<SSTable> level = levels.get(i);

                if (level.size() > maxLevelSize) {
                    // add new level if needed
                    if (i == n - 1)
                        levels.add(new ObjectArrayList<>());

                    // take all tables from the current and next level
                    ObjectArrayList<SSTable> nextLevel = levels.get(i + 1);
                    ObjectArrayList<SSTable> merge = new ObjectArrayList<>();
                    merge.addAll(level);
                    merge.addAll(nextLevel);

                    // perform a sorted run and replace the next level
                    var sortedRun = SSTable.sortedRun(dataDir, sstMaxSize, merge.toArray(SSTable[]::new));

                    // delete previous tables
                    level.forEach(SSTable::closeAndDelete);
                    level.clear();
                    nextLevel.forEach(SSTable::closeAndDelete);
                    nextLevel.clear();

                    nextLevel.addAll(sortedRun);
                }

                maxLevelSize = (int) (maxLevelSize * LEVEL_INCR_FACTOR);
                sstMaxSize = (int) (sstMaxSize * LEVEL_INCR_FACTOR);
            }
        }
    }

    private void createDataDir() {
        try {
            Files.createDirectory(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("Could not create data directory", e);
        }
    }


    @Override
    public String toString() {

        var s = new StringBuilder();
        s.append("LSM-Tree {\n");
        s.append("\tmemtable: ");
        s.append(mutableMemtable.byteSize() / 1024.0 / 1024.0);
        s.append(" mb\n");
        s.append("\timmutable memtables: ");
        s.append(immutableMemtables);
        s.append("\n\tsst levels:\n");

        int i = 0;
        for (var level : levels) {
            s.append(String.format("\t\t- %d: ", i));
            level.stream()
                 .map(st -> String.format("[ %s, size: %d ] ", st.filename, st.size))
                 .forEach(s::append);
            s.append("\n");
            i += 1;
        }

        s.append("}");
        return s.toString();
    }
}
