package com.tomfran.lsm.sstable;

import com.tomfran.lsm.bloom.BloomFilter;
import com.tomfran.lsm.io.BaseInputStream;
import com.tomfran.lsm.io.BaseOutputStream;
import com.tomfran.lsm.io.ItemsInputStream;
import com.tomfran.lsm.io.ItemsOutputStream;
import com.tomfran.lsm.types.Item;
import com.tomfran.lsm.utils.IteratorMerger;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SSTable {

    public static final String DATA_FILE_EXTENSION = ".data";
    public static final String BLOOM_FILE_EXTENSION = ".bloom";
    public static final String INDEX_FILE_EXTENSION = ".index";

    ItemsInputStream is;
    int size;

    LongArrayList sparseOffsets;
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
    public SSTable(String filename, Iterable<Item> items, int sampleSize, int numItems) {
        writeItems(filename, items, sampleSize, numItems);
        is = new ItemsInputStream(filename + DATA_FILE_EXTENSION);
    }

    /**
     * Initialize an SSTable from disk.
     *
     * @param filename The base filename of the SSTable.
     */
    public SSTable(String filename) {
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
        Iterator<Item>[] iterators = new Iterator[tables.length];
        for (int i = 0; i < tables.length; i++) {
            iterators[i] = tables[i].iterator();
            newSize += tables[i].size;
        }

        SSTableMergerIterator it = new SSTableMergerIterator(iterators);

        return new SSTable(filename, it, sampleSize, newSize);
    }

    private void initializeFromDisk(String filename) {
        // items file
        is = new ItemsInputStream(filename + DATA_FILE_EXTENSION);

        // sparse index
        sparseOffsets = new LongArrayList();
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
    public Item getItem(byte[] key) {
        if (!bloomFilter.mightContain(key))
            return null;

        long offset = getCandidateOffset(key);
        is.seek(offset);

        Item it;
        int cmp = 1;

        while ((it = is.readItem()) != null &&
                it.key().length > 0 &&
                (cmp = compare(key, it.key())) > 0) {
        }

        return cmp == 0 ? it : null;
    }

    /**
     * Get an iterator over the items in the SSTable.
     *
     * @return Table iterator
     */
    public Iterator<Item> iterator() {
        is.seek(0);
        return new SSTableIterator(this);
    }

    /**
     * Close the SSTable input stream.
     */
    public void close() {
        is.close();
    }

    private long getCandidateOffset(byte[] key) {
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
                return sparseOffsets.getLong(mid);
        }
        return sparseOffsets.getLong(low);
    }

    private void writeItems(String filename, Iterable<Item> items, int sampleSize, int numItems) {
        ItemsOutputStream ios = new ItemsOutputStream(filename + DATA_FILE_EXTENSION);

        sparseOffsets = new LongArrayList();
        sparseKeys = new ObjectArrayList<>();
        bloomFilter = new BloomFilter(numItems);

        // write items and populate indexes
        int size = 0;
        long offset = 0L;
        for (Item item : items) {
            if (size % sampleSize == 0) {
                sparseOffsets.add(offset);
                sparseKeys.add(item.key());
            }
            bloomFilter.add(item.key());

            offset += ios.writeItem(item);
            size++;
        }
        ios.close();

        this.size = size;

        // write bloom filter and index to disk
        bloomFilter.writeToFile(filename + BLOOM_FILE_EXTENSION);

        BaseOutputStream indexOs = new BaseOutputStream(filename + INDEX_FILE_EXTENSION);
        indexOs.writeVByteInt(size);
        indexOs.writeVByteInt(sparseOffsets.size());

        // skip first offset, always 0
        long prev = 0L;
        for (int i = 1; i < sparseOffsets.size(); i++) {
            indexOs.writeVByteLong(sparseOffsets.getLong(i) - prev);
            prev = sparseOffsets.getLong(i);
        }

        for (byte[] key : sparseKeys) {
            indexOs.writeVByteInt(key.length);
            indexOs.write(key);
        }

        indexOs.close();
    }

    private static class SSTableIterator implements Iterator<Item> {

        private final SSTable table;

        public SSTableIterator(SSTable table) {
            this.table = table;
        }

        @Override
        public boolean hasNext() {
            return table.is.hasNext();
        }

        @Override
        public Item next() {
            return table.is.readItem();
        }
    }

    /**
     * SSTableMergerIterator is an IteratorMerger that merges SSTables.
     * <p>
     * When merging SSTables, we want to skip over duplicate keys. This is done by
     * keeping track of the last key we saw, and skipping over any keys that are
     * equal to the last key.
     */
    private static class SSTableMergerIterator extends IteratorMerger<Item> implements Iterable<Item> {

        private Item last, next;

        @SafeVarargs
        public SSTableMergerIterator(Iterator<Item>... iterators) {
            super((a, b) -> compare(a.key(), b.key()), iterators);
            last = super.next();
        }

        @Override
        public boolean hasNext() {
            return last != null;
        }

        @Override
        public Item next() {
            next = super.next();
            while (next != null && compare(last.key(), next.key()) == 0)
                next = super.next();

            Item toReturn = last;
            last = next;

            return toReturn;
        }

        @Override
        public Iterator<Item> iterator() {
            return this;
        }
    }

}
