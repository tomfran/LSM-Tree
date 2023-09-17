package com.tomfran.lsm.memtable;

import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.Item;

import java.util.LinkedList;

public class Memtable {

    SkipList mutableData;
    LinkedList<SkipList> immutableData;
    LinkedList<LinkedList<SSTable>> sstables;

    public Memtable() {
        mutableData = new SkipList();
        immutableData = new LinkedList<>();
    }

    public void add(Item item) {
        mutableData.add(item);
    }

    public void get(byte[] key) {
        mutableData.get(key);
    }

    public void remove(byte[] key) {
        mutableData.remove(key);
    }

    public int size() {
        return mutableData.size();
    }

    private void replaceMutableData() {
        immutableData.addFirst(mutableData);
        mutableData = new SkipList();
    }

    public SSTable flush() {
        return null;
    }

}
