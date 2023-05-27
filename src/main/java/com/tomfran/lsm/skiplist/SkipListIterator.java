package com.tomfran.lsm.skiplist;

import com.tomfran.lsm.iterator.Iterator;

public class SkipListIterator implements Iterator {

    private Node curr;

    public SkipListIterator(SkipList skipList) {
        this.curr = skipList.sentinel;
    }

    @Override
    public boolean hasNext() {
        return curr.next[0] != null;
    }

    @Override
    public void next() {
        curr = curr.next[0];
    }

    @Override
    public byte[] key() {
        return curr.key;
    }

    @Override
    public byte[] value() {
        return curr.value;
    }
}
