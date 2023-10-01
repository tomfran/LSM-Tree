package com.tomfran.lsm.memtable;

import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.Item;

public class Memtable {

    static final int DEFAULT_SAMPLE_SIZE = 1000;

    SkipList list;


    public Memtable() {
        list = new SkipList();
    }

    public void add(Item item) {
        list.add(item);
    }

    public void get(byte[] key) {
        list.get(key);
    }

    public void remove(byte[] key) {
        list.add(new Item(key, null));
    }

    public int size() {
        return list.size();
    }

    public SSTable flush() {
        String filename = "sstable-" + System.currentTimeMillis();
        return new SSTable(filename, list, DEFAULT_SAMPLE_SIZE, list.size());
    }

}
