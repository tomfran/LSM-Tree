package com.tomfran.lsm.sstable;

import com.tomfran.lsm.bloom.BloomFilter;
import com.tomfran.lsm.io.ItemsInputStream;
import com.tomfran.lsm.io.ItemsOutputStream;
import com.tomfran.lsm.types.Item;
import com.tomfran.lsm.utils.IteratorMerger;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SSTable {

    private final ItemsInputStream is;
    private int size;

    private LongArrayList sparseIndex;
    private ObjectArrayList<byte[]> sparseKeys;
    private BloomFilter bloomFilter;

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
        is = new ItemsInputStream(filename);
    }

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

    /**
     * Read an item from the SSTable.
     *
     * @param key The key of the item to read.
     * @return The item with the given key, or null if no such item exists.
     */
    public Item getItem(byte[] key) {
        if (!bloomFilter.mightContain(key))
            return null;

        is.seek(getCandidateOffset(key));

        Item it;
        int cmp = -1;

        while ((it = is.readItem()) != null && (cmp = compare(key, it.key())) > 0) ;

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
        int high = sparseIndex.size() - 1;

        while (low < (high - 1)) {
            int mid = (low + high) / 2;
            int cmp = compare(key, sparseKeys.get(mid));

            if (cmp < 0)
                high = mid - 1;
            else if (cmp > 0)
                low = mid;
            else
                return sparseIndex.getLong(mid);
        }
        return sparseIndex.getLong(low);
    }

    private void writeItems(String filename, Iterable<Item> items, int sampleSize, int numItems) {
        ItemsOutputStream fos = new ItemsOutputStream(filename);

        sparseIndex = new LongArrayList();
        sparseKeys = new ObjectArrayList<>();
        bloomFilter = new BloomFilter(numItems);

        int size = 0;
        long offset = 0L;

        for (Item item : items) {
            if (size % sampleSize == 0) {
                sparseIndex.add(offset);
                sparseKeys.add(item.key());
            }
            bloomFilter.add(item.key());

            offset += fos.writeItem(item);
            size++;
        }
        this.size = size;

        fos.close();
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
