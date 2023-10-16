package com.tomfran.lsm.sstable;

import com.tomfran.lsm.bloom.BloomFilter;
import com.tomfran.lsm.io.BaseInputStream;
import com.tomfran.lsm.io.BaseOutputStream;
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

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SSTable implements Iterable<ByteArrayPair> {

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
     */
    public SSTable(String filename, Iterator<ByteArrayPair> items, int sampleSize) {
        this.filename = filename;
        writeItems(filename, items, sampleSize);
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
    public static SSTable merge(String filename, int sampleSize, Iterable<ByteArrayPair>... tables) {
        Iterator<ByteArrayPair>[] itArray = Arrays.stream(tables).map(Iterable::iterator)
                                                  .toArray(Iterator[]::new);

        IteratorMerger<ByteArrayPair> merger = new IteratorMerger<>(itArray);
        UniqueSortedIterator<ByteArrayPair> uniqueSortedIterator = new UniqueSortedIterator<>(merger);

        return new SSTable(filename, uniqueSortedIterator, sampleSize);
    }

    public static SSTable merge(String filename, int sampleSize, LinkedList<SSTable> tableLinkedList) {
        return merge(filename, sampleSize, tableLinkedList.toArray(new Iterable[]{}));
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

    public void deleteFiles() {
        for (var extension : List.of(DATA_FILE_EXTENSION, INDEX_FILE_EXTENSION, BLOOM_FILE_EXTENSION))
            new File(filename + extension).delete();
    }

    public void closeAndDelete() {
        close();
        deleteFiles();
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

    private void writeItems(String filename, Iterator<ByteArrayPair> items, int sampleSize) {
        BaseOutputStream ios = new BaseOutputStream(filename + DATA_FILE_EXTENSION);

        sparseOffsets = new LongArrayList();
        sparseSizeCount = new IntArrayList();
        sparseKeys = new ObjectArrayList<>();
        bloomFilter = new BloomFilter();

        // write items and populate indexes
        int size = 0;
        long offset = 0L;
        while (items.hasNext()) {
            ByteArrayPair item = items.next();
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

        if (size == 0) {
            throw new IllegalArgumentException("Attempted to create an SSTable from an empty iterator");
        }

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

}
