package com.tomfran.lsm.tree;

import com.tomfran.lsm.compactor.SSTableCompactor;
import com.tomfran.lsm.skiplist.Memtable;
import com.tomfran.lsm.sstable.SSTable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.LinkedList;

public class LSMTree {

    LinkedList<Memtable> memtables;
    ObjectArrayList<LinkedList<SSTable>> ssTables;

    public LSMTree() {
        memtables = new LinkedList<>();
        addMemtable();

        ssTables = new ObjectArrayList<>();
    }

    public void put(byte[] key, byte[] value) {
        memtables.getFirst().put(key, value);
    }

    public byte[] get(byte[] key) {
        byte[] value = searchMemtables(key);

        if (value == null)
            value = searchSSTables(key);

        return value;
    }

    private byte[] searchMemtables(byte[] key) {
        for (Memtable m : memtables) {
            byte[] value = m.get(key);
            if (value != null)
                return value;
        }
        return null;
    }

    private byte[] searchSSTables(byte[] key) {
        for (LinkedList<SSTable> tables : ssTables) {
            for (SSTable table : tables) {
                byte[] value = table.get(key);
                if (value != null)
                    return value;
            }
        }
        return null;
    }

    public void addMemtable() {
        memtables.addFirst(new Memtable());
    }

    public void flushLastMemtable() {
        Memtable m = memtables.getLast();
        SSTable table = m.flush();

        if (ssTables.isEmpty())
            ssTables.add(new LinkedList<>());

        memtables.removeLast();
        ssTables.get(0).addFirst(table);
    }

    public void compact(int level) {
        if (level < 0 || level >= ssTables.size())
            throw new RuntimeException("Invalid level");

        LinkedList<SSTable> tables = ssTables.get(level);
        if (tables.size() < 2)
            return;

        SSTableCompactor compactor = new SSTableCompactor(tables);
        SSTable compacted = compactor.compact();

        ssTables.get(level + 1).addFirst(compacted);
        ssTables.get(level).clear();
    }

}
