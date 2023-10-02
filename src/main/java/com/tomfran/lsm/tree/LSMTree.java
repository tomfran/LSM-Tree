package com.tomfran.lsm.tree;

import com.tomfran.lsm.memtable.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.ByteArrayPair;

import java.util.LinkedList;

public class LSMTree {

    static final int MEMTABLE_MAX_SIZE = 1 << 16;

    Memtable mutableMemtable;
    LinkedList<Memtable> immutableMemtables;
    LinkedList<SSTable> tables;

    public LSMTree() {
        mutableMemtable = new Memtable(MEMTABLE_MAX_SIZE);
        immutableMemtables = new LinkedList<>();
        tables = new LinkedList<>();
    }

    public void add(ByteArrayPair item) {
        mutableMemtable.add(item);
        checkMemtableSize();
    }

    public void delete(byte[] key) {
        mutableMemtable.remove(key);
        checkMemtableSize();
    }

    private void checkMemtableSize() {
        if (mutableMemtable.size() >= MEMTABLE_MAX_SIZE) {
            immutableMemtables.add(mutableMemtable);
            mutableMemtable = new Memtable(MEMTABLE_MAX_SIZE);
        }
    }

    public ByteArrayPair get(byte[] key) {
        ByteArrayPair result;

        if ((result = mutableMemtable.get(key)) != null)
            return result;

        for (Memtable memtable : immutableMemtables)
            if ((result = memtable.get(key)) != null)
                return result;

        byte[] tmp;
        for (SSTable table : tables)
            if ((tmp = table.get(key)) != null)
                return new ByteArrayPair(key, tmp);

        return null;
    }
}
