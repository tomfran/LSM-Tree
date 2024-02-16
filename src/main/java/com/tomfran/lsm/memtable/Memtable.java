package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.UniqueSortedIterator;

import java.util.Iterator;

public class Memtable implements Iterable<ByteArrayPair> {

    SkipList list;
    long byteSize;

    /**
     * Initialize a Memtable with default list size.
     */
    public Memtable() {
        list = new SkipList();
        byteSize = 0L;
    }

    /**
     * Add an item to the underlying list.
     *
     * @param item the item to add.
     */
    public void add(ByteArrayPair item) {
        list.add(item);
        byteSize += item.size();
    }

    /**
     * Retrieve an item from the underlying list.
     *
     * @param key the key of the wanted element.
     * @return the found element or null.
     */
    public byte[] get(byte[] key) {
        return list.get(key);
    }

    /**
     * Remove an element by inserting a tombstone.
     *
     * @param key the key of the element to remove.
     */
    public void remove(byte[] key) {
        list.add(new ByteArrayPair(key, new byte[]{}));
    }

    /**
     * Return the size in bytes of the skiplist.
     *
     * @return bytes indicating size of underlying list.
     */
    public long byteSize() {
        return byteSize;
    }

    /**
     * Returns an iterator discarding duplicated elements.
     *
     * @return modified underlying list iterator.
     */
    @Override
    public Iterator<ByteArrayPair> iterator() {
        return new UniqueSortedIterator<>(list.iterator());
    }

}
