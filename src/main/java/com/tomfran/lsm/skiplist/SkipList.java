package com.tomfran.lsm.skiplist;

import com.tomfran.lsm.comparator.ByteArrayComparator;

import java.util.random.RandomGenerator;

public class SkipList {

    protected final Node sentinel;
    protected final int levels;
    protected final RandomGenerator rn = RandomGenerator.getDefault();
    protected int size;
    protected int nonNullSize;

    public SkipList(int levels) {
        if (levels < 1)
            throw new RuntimeException("SkipList must have at least one level");

        this.levels = levels;
        this.sentinel = new Node(null, null, new Node[levels]);
    }

    private int compare(byte[] a, byte[] b) {
        return ByteArrayComparator.compare(a, b);
    }

    public void put(byte[] key, byte[] value) {
        Node[] toUpdate = new Node[levels];
        Node curr = sentinel;
        for (int l = levels - 1; l >= 0; l--) {
            while (curr.next[l] != null && compare(curr.next[l].key, key) < 0)
                curr = curr.next[l];

            toUpdate[l] = curr;
        }

        if (toUpdate[0].next[0] != null && compare(toUpdate[0].next[0].key, key) == 0) {
            toUpdate[0].next[0].value = value;
            return;
        }

        Node inserted = new Node(key, value, new Node[levels]);

        boolean nextLevel = true;
        for (int l = 0; l < levels && nextLevel; l++) {
            curr = toUpdate[l];
            Node next = curr.next[l];

            curr.next[l] = inserted;
            inserted.next[l] = next;

            nextLevel = (rn.nextInt(0, 10) % 2) == 1;
        }
        size++;
        if (value != null)
            nonNullSize++;
    }

    public byte[] get(byte[] key) {
        Node curr = sentinel;
        for (int l = levels - 1; l >= 0; l--) {
            while (curr.next[l] != null && compare(curr.next[l].key, key) < 0)
                curr = curr.next[l];
        }

        if (curr.next[0] != null && compare(curr.next[0].key, key) == 0)
            return curr.next[0].value;
        return null;
    }

    public void delete(byte[] key) {
        put(key, null);
        nonNullSize--;
    }

    public int size() {
        return size;
    }

    public SkipListIterator iterator() {
        return new SkipListIterator(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int l = levels - 1; l >= 0; l--) {
            sb.append("Level ").append(l).append(": ");
            Node curr = sentinel;
            while (curr.next[l] != null) {
                sb.append(curr.next[l].toString()).append(" -> ");
                curr = curr.next[l];
            }
            sb.append("null\n");
        }
        return sb.toString();
    }

}