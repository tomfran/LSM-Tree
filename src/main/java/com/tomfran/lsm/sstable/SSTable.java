package com.tomfran.lsm.sstable;

import com.tomfran.lsm.bloom.BloomFilter;
import com.tomfran.lsm.io.BaseInputStream;
import com.tomfran.lsm.io.BaseOutputStream;
import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.IteratorMerger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SSTable {

    public static final String DATA_FILE_EXTENSION = ".data";
    public static final String BLOOM_FILE_EXTENSION = ".bloom";
    public static final String INDEX_FILE_EXTENSION = ".index";

    String filename;
    BaseInputStream is;
    int size;

    LongArrayList sparseOffsets;
    IntArrayList sparseSizeCount;
    ObjectArrayList<byte[]> sparseKeys;
    BloomFilter bloomFilter;

    /**
     * Create a new SSTable from an Iterable of Items.
     *
     * @param filename   The filename to write the SSTable to.
     * @param items      The items to write to the SSTable, assumed to be sorted.
     * @param sampleSize The number of items to skip between sparse index entries.
     * @param numItems   The number of items in the SSTable.
     */
    public SSTable(String filename, Iterable<ByteArrayPair> items, int sampleSize, int numItems) {
        this.filename = filename;
        writeItems(filename, items, sampleSize, numItems);
        is = new BaseInputStream(filename + DATA_FILE_EXTENSION);
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

    /**
     * Merge multiple SSTables into a single SSTable.
     *
     * @param filename   The filename to write the SSTable to.
     * @param sampleSize The number of items to skip between sparse index entries.
     * @param tables     The SSTables to merge.
     * @return The merged SSTable.
     */
    static SSTable merge(String filename, int sampleSize, SSTable... tables) {

        int newSize = 0;
        Iterator<ByteArrayPair>[] iterators = new Iterator[tables.length];
        for (int i = 0; i < tables.length; i++) {
            iterators[i] = tables[i].iterator();
            newSize += tables[i].size;
        }

        SSTableMergerIterator it = new SSTableMergerIterator(iterators);

        return new SSTable(filename, it, sampleSize, newSize);
    }

    private void initializeFromDisk(String filename) {
        // items file
        is = new BaseInputStream(filename + DATA_FILE_EXTENSION);

        // sparse index
        sparseOffsets = new LongArrayList();
        sparseSizeCount = new IntArrayList();
        sparseKeys = new ObjectArrayList<>();

        BaseInputStream indexIs = new BaseInputStream(filename + INDEX_FILE_EXTENSION);
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
        for (int i = 0; i < sparseSize; i++) {
            sizeCumulative += indexIs.readVByteInt();
            sparseSizeCount.add(sizeCumulative);
        }

        for (int i = 0; i < sparseSize; i++)
            sparseKeys.add(indexIs.readNBytes(indexIs.readVByteInt()));

        is.close();

        // bloom filter
        bloomFilter = BloomFilter.readFromFile(filename + BLOOM_FILE_EXTENSION);
    }

    /**
     * Read an item from the SSTable.
     *
     * @param key The key of the item to read.
     * @return The item with the given key, or null if no such item exists.
     */
    public byte[] get(byte[] key) {
        if (!bloomFilter.mightContain(key))
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

    private void writeItems(String filename, Iterable<ByteArrayPair> items, int sampleSize, int numItems) {
        BaseOutputStream ios = new BaseOutputStream(filename + DATA_FILE_EXTENSION);

        sparseOffsets = new LongArrayList();
        sparseSizeCount = new IntArrayList();
        sparseKeys = new ObjectArrayList<>();
        bloomFilter = new BloomFilter(numItems);

        // write items and populate indexes
        int size = 0;
        long offset = 0L;
        for (ByteArrayPair item : items) {
            if (size % sampleSize == 0) {
                sparseOffsets.add(offset);
                sparseSizeCount.add(size);
                sparseKeys.add(item.key());
            }
            bloomFilter.add(item.key());

            offset += ios.writeBytePair(item);
            size++;
        }
        ios.close();

        this.size = size;

        // write bloom filter and index to disk
        bloomFilter.writeToFile(filename + BLOOM_FILE_EXTENSION);

        BaseOutputStream indexOs = new BaseOutputStream(filename + INDEX_FILE_EXTENSION);
        indexOs.writeVByteInt(size);

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

    /**
     * SSTableMergerIterator is an IteratorMerger that merges SSTables.
     * <p>
     * When merging SSTables, we want to skip over duplicate keys. This is done by
     * keeping track of the last key we saw, and skipping over any keys that are
     * equal to the last key.
     */
    private static class SSTableMergerIterator extends IteratorMerger<ByteArrayPair> implements Iterable<ByteArrayPair> {

        private ByteArrayPair last;

        @SafeVarargs
        public SSTableMergerIterator(Iterator<ByteArrayPair>... iterators) {
            super(iterators);
            last = super.next();
        }

        @Override
        public boolean hasNext() {
            return last != null;
        }

        @Override
        public ByteArrayPair next() {
            ByteArrayPair next = super.next();
            while (next != null && last.compareTo(next) == 0)
                next = super.next();

            ByteArrayPair toReturn = last;
            last = next;

            return toReturn;
        }

        @Override
        public Iterator<ByteArrayPair> iterator() {
            return this;
        }

    }

}
