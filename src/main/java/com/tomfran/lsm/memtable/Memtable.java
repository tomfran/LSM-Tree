package com.tomfran.lsm.memtable;

import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.ByteArrayPair;

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

    public SSTable flush(String filename, int sampleSize) {
        return new SSTable(filename, list, sampleSize);
    }

    @Override
    public Iterator<ByteArrayPair> iterator() {
        return list.iterator();
    }

}
