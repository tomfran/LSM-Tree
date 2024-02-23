package com.tomfran.lsm.sstable;

import com.tomfran.lsm.bloom.BloomFilter;
import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.io.ExtendedInputStream;
import com.tomfran.lsm.io.ExtendedOutputStream;
import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.IteratorMerger;
import com.tomfran.lsm.utils.UniqueSortedIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SSTable implements Iterable<ByteArrayPair> {

    public static final String DATA_FILE_EXTENSION = ".data";
    public static final String BLOOM_FILE_EXTENSION = ".bloom";
    public static final String INDEX_FILE_EXTENSION = ".index";

    private static final int DEFAULT_SAMPLE_SIZE = 1000;

    static final AtomicLong SST_COUNTER = new AtomicLong();

    public String filename;
    ExtendedInputStream is;
    public int size;

    LongArrayList sparseOffsets;
    IntArrayList sparseSizeCount;
    ObjectArrayList<byte[]> sparseKeys;
    BloomFilter bloomFilter;

    byte[] minKey;
    byte[] maxKey;

    /**
     * Create a new SSTable from an Iterable of Items.
     *
     * @param filename   The filename to write the SSTable to.
     * @param items      The items to write to the SSTable, assumed to be sorted.
     * @param sampleSize The number of items to skip between sparse index entries.
     */
    public SSTable(String directory, Iterator<ByteArrayPair> items, int sampleSize) {
        this(getNextSstFilename(directory), items, sampleSize, 1024 * 1024 * 256);
    }

    public SSTable(String directory, Iterator<ByteArrayPair> items) {
        this(getNextSstFilename(directory), items, DEFAULT_SAMPLE_SIZE, 1024 * 1024 * 256);
    }

    public SSTable(String directory, Iterator<ByteArrayPair> items, long maxByteSize) {
        this(getNextSstFilename(directory), items, DEFAULT_SAMPLE_SIZE, maxByteSize);
    }


    public SSTable(String filename, Iterator<ByteArrayPair> items, int sampleSize, long maxByteSize) {
        this.filename = filename;
        writeItems(filename, items, sampleSize, maxByteSize);
        is = new ExtendedInputStream(filename + DATA_FILE_EXTENSION);
    }

    /**
     * Initialize an SSTable from disk.
     *
     * @param filename The base filename of the SSTable.
     */
    public SSTable(String filename) {
        this.filename = filename;
        initializeFromDisk(filename);
    }

    public static ObjectArrayList<SSTable> sortedRun(String dataDir, long sstMaxSize, SSTable... tables) {
        SSTableIterator[] itArray = Arrays.stream(tables).map(SSTable::iterator).toArray(SSTableIterator[]::new);

        IteratorMerger<ByteArrayPair> merger = new IteratorMerger<>(itArray);
        UniqueSortedIterator<ByteArrayPair> uniqueSortedIterator = new UniqueSortedIterator<>(merger);

        ObjectArrayList<SSTable> res = new ObjectArrayList<>();

        while (uniqueSortedIterator.hasNext()) {
            res.add(new SSTable(getNextSstFilename(dataDir), uniqueSortedIterator, DEFAULT_SAMPLE_SIZE, sstMaxSize));
        }

        return res;
    }

    /**
     * Read an item from the SSTable.
     *
     * @param key The key of the item to read.
     * @return The item with the given key, or null if no such item exists.
     */
    public byte[] get(byte[] key) {
        if (ByteArrayComparator.compare(key, minKey) == -1 ||
            ByteArrayComparator.compare(key, maxKey) == 1 ||
            !bloomFilter.mightContain(key))
            return null;

        int offsetIndex = getCandidateOffsetIndex(key);
        long offset = sparseOffsets.getLong(offsetIndex);
        int remaining = size - sparseSizeCount.getInt(offsetIndex);
        is.seek(offset);

        int cmp = 1;
        int searchKeyLen = key.length, readKeyLen, readValueLen;

        byte[] readKey;

        while (cmp > 0 && remaining > 0) {

            remaining--;
            readKeyLen = is.readVByteInt();

            // gone too far
            if (readKeyLen > searchKeyLen) {
                return null;
            }

            // gone too short
            if (readKeyLen < searchKeyLen) {
                readValueLen = is.readVByteInt();
                is.skip(readKeyLen + readValueLen);
                continue;
            }

            // read full key, compare, if equal read value
            readValueLen = is.readVByteInt();
            readKey = is.readNBytes(readKeyLen);
            cmp = compare(key, readKey);

            if (cmp == 0) {
                return is.readNBytes(readValueLen);
            } else {
                is.skip(readValueLen);
            }
        }

        return null;
    }

    /**
     * Get an iterator over the items in the SSTable.
     *
     * @return Table iterator
     */
    public Iterator<ByteArrayPair> iterator() {
        is.seek(0);
        return new SSTableIterator(this);
    }

    /**
     * Close the SSTable input stream.
     */
    public void close() {
        is.close();
    }

    public void deleteFiles() {
        for (var extension : List.of(DATA_FILE_EXTENSION, INDEX_FILE_EXTENSION, BLOOM_FILE_EXTENSION))
            new File(filename + extension).delete();
    }

    public void closeAndDelete() {
        close();
        deleteFiles();
    }

    private static String getNextSstFilename(String directory) {
        return String.format("%s/sst_%d", directory, SST_COUNTER.incrementAndGet());
    }

    private void initializeFromDisk(String filename) {
        // items file
        is = new ExtendedInputStream(filename + DATA_FILE_EXTENSION);

        // sparse index
        sparseOffsets = new LongArrayList();
        sparseSizeCount = new IntArrayList();
        sparseKeys = new ObjectArrayList<>();

        ExtendedInputStream indexIs = new ExtendedInputStream(filename + INDEX_FILE_EXTENSION);
        size = indexIs.readVByteInt();

        int sparseSize = indexIs.readVByteInt();
        long offsetsCumulative = 0;
        sparseOffsets.add(offsetsCumulative);
        for (int i = 0; i < sparseSize - 1; i++) {
            offsetsCumulative += indexIs.readVByteLong();
            sparseOffsets.add(offsetsCumulative);
        }

        int sizeCumulative = 0;
        sparseSizeCount.add(sizeCumulative);
        for (int i = 0; i < sparseSize - 1; i++) {
            sizeCumulative += indexIs.readVByteInt();
            sparseSizeCount.add(sizeCumulative);
        }

        for (int i = 0; i < sparseSize; i++)
            sparseKeys.add(indexIs.readNBytes(indexIs.readVByteInt()));

        is.close();

        // bloom filter
        bloomFilter = BloomFilter.readFromFile(filename + BLOOM_FILE_EXTENSION);
    }

    private int getCandidateOffsetIndex(byte[] key) {
        int low = 0;
        int high = sparseOffsets.size() - 1;

        while (low < (high - 1)) {
            int mid = (low + high) / 2;
            int cmp = compare(key, sparseKeys.get(mid));

            if (cmp < 0)
                high = mid - 1;
            else if (cmp > 0)
                low = mid;
            else
                return mid;
        }
        return low;
    }

    private void writeItems(String filename, Iterator<ByteArrayPair> items, int sampleSize, long maxByteSize) {
        ExtendedOutputStream ios = new ExtendedOutputStream(filename + DATA_FILE_EXTENSION);

        sparseOffsets = new LongArrayList();
        sparseSizeCount = new IntArrayList();
        sparseKeys = new ObjectArrayList<>();
        bloomFilter = new BloomFilter();

        // write items and populate indexes
        int numElements = 0;
        long offset = 0L;
        long byteSize = 0L;

        while (items.hasNext() && byteSize < maxByteSize) {
            ByteArrayPair item = items.next();

            if (minKey == null)
                minKey = item.key();

            maxKey = item.key();

            if (numElements % sampleSize == 0) {
                sparseOffsets.add(offset);
                sparseSizeCount.add(numElements);
                sparseKeys.add(item.key());
            }

            bloomFilter.add(item.key());

            offset += ios.writeByteArrayPair(item);
            numElements++;

            byteSize += item.size();
        }

        ios.close();

        if (numElements == 0) {
            throw new IllegalArgumentException("Attempted to create an SSTable from an empty iterator");
        }

        this.size = numElements;

        // write bloom filter and index to disk
        bloomFilter.writeToFile(filename + BLOOM_FILE_EXTENSION);

        ExtendedOutputStream indexOs = new ExtendedOutputStream(filename + INDEX_FILE_EXTENSION);
        indexOs.writeVByteInt(numElements);

        int sparseSize = sparseOffsets.size();
        indexOs.writeVByteInt(sparseSize);

        // skip first offset, always 0
        long prevOffset = 0L;
        for (int i = 1; i < sparseSize; i++) {
            indexOs.writeVByteLong(sparseOffsets.getLong(i) - prevOffset);
            prevOffset = sparseOffsets.getLong(i);
        }

        int prevSize = 0;
        for (int i = 1; i < sparseSize; i++) {
            indexOs.writeVByteInt(sparseSizeCount.getInt(i) - prevSize);
            prevSize = sparseSizeCount.getInt(i);
        }

        for (byte[] key : sparseKeys) {
            indexOs.writeVByteInt(key.length);
            indexOs.write(key);
        }

        indexOs.close();
    }

    private static class SSTableIterator implements Iterator<ByteArrayPair> {

        private final SSTable table;
        int remaining;

        public SSTableIterator(SSTable table) {
            this.table = table;
            remaining = table.size;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public ByteArrayPair next() {
            remaining--;

            return table.is.readBytePair();
        }

    }

}
