package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.UniqueSortedIterator;

import java.util.Iterator;

public class Memtable implements Iterable<ByteArrayPair> {

    SkipList list;

    public Memtable() {
        list = new SkipList();
    }

    public Memtable(int numElements) {
        list = new SkipList(numElements);
    }

    public void add(ByteArrayPair item) {
        list.add(item);
    }

    public byte[] get(byte[] key) {
        return list.get(key);
    }

    public void remove(byte[] key) {
        list.add(new ByteArrayPair(key, new byte[]{}));
    }

    public int size() {
        return list.size();
    }

    @Override
    public Iterator<ByteArrayPair> iterator() {
        return new UniqueSortedIterator<>(list.iterator());
    }

}
