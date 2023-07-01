package com.tomfran.lsm.skiplist;

import com.tomfran.lsm.iterator.Iterator;
import com.tomfran.lsm.sstable.SSTable;

public class Memtable {

    static final int SKIPLIST_LEVELS = 10;

    private final SkipList list;

    public Memtable() {
        list = new SkipList(SKIPLIST_LEVELS);
    }

    public void put(byte[] key, byte[] value) {
        list.put(key, value);
    }

    public byte[] get(byte[] key) {
        return list.get(key);
    }

    public int size() {
        return list.size();
    }

    public Iterator iterator() {
        return new SkipListIterator(list);
    }

    public SSTable flush() {
        Iterator it = iterator();

        SSTable table = new SSTable(list.size());

        while (it.hasNext()) {
            it.next();
            table.put(it.key(), it.value());
        }

        return table;
    }
}
