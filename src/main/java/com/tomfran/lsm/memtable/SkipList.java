package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import java.util.Iterator;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;
import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * A skip list implementation of items.
 */
public class SkipList implements Iterable<ByteArrayPair> {

    static final int DEFAULT_ELEMENTS = 1 << 16;

    final Node sentinel;

    private final Node[] buffer;
    private final XoRoShiRo128PlusRandom rn;

    int levels;
    int size;

    /**
     * Create a skip list with a default number of elements, 2 ^ 16.
     */
    public SkipList() {
        this(DEFAULT_ELEMENTS);
    }

    /**
     * Create a skip list with a specified number of elements.
     *
     * @param numElements The number of elements to size the skip list for.
     */
    public SkipList(int numElements) {
        levels = (int) ceil(log(numElements) / log(2));
        size = 0;
        sentinel = new Node(null, levels);
        rn = new XoRoShiRo128PlusRandom();
        buffer = new Node[levels];
    }

    /**
     * Add an item to the skip list.
     *
     * @param item The item to add.
     */
    public void add(ByteArrayPair item) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && current.next[i].val.compareTo(item) < 0)
                current = current.next[i];
            buffer[i] = current;
        }

        if (current.next[0] != null && current.next[0].val.compareTo(item) == 0) {
            current.next[0].val = item;
            return;
        }

        Node newNode = new Node(item, levels);
        for (int i = 0; i < randomLevel(); i++) {
            newNode.next[i] = buffer[i].next[i];
            buffer[i].next[i] = newNode;
        }
        size++;
    }

    private int randomLevel() {
        int level = 1;
        long n = rn.nextLong();
        while (level < levels && (n & (1L << level)) != 0)
            level++;
        return level;
    }

    /**
     * Retrieve an item from the skip list.
     *
     * @param key The key of the item to retrieve.
     * @return The item if found, null otherwise.
     */
    public byte[] get(byte[] key) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && compare(current.next[i].val.key(), key) < 0)
                current = current.next[i];
        }

        if (current.next[0] != null && compare(current.next[0].val.key(), key) == 0)
            return current.next[0].val.value();

        return null;
    }

    /**
     * Remove an item from the skip list.
     *
     * @param key The key of the item to remove.
     */
    public void remove(byte[] key) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && compare(current.next[i].val.key(), key) < 0)
                current = current.next[i];
            buffer[i] = current;
        }

        if (current.next[0] != null && compare(current.next[0].val.key(), key) == 0) {
            boolean last = current.next[0].next[0] == null;
            for (int i = 0; i < levels; i++) {
                if (buffer[i].next[i] != current.next[0])
                    break;
                buffer[i].next[i] = last ? null : current.next[0].next[i];
            }
            size--;
        }
    }

    /**
     * Get the number of items in the skip list.
     *
     * @return Skip list size.
     */
    public int size() {
        return size;
    }

    /**
     * Get an iterator over the items in the skip list at the lowest level.
     *
     * @return An iterator over the items in the skip list.
     */
    @Override
    public Iterator<ByteArrayPair> iterator() {
        return new SkipListIterator(sentinel);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = levels - 1; i >= 0; i--) {
            sb.append(String.format("Level %2d: ", i));
            Node current = sentinel;
            while (current.next[i] != null) {
                sb.append(current.next[i].val).append(" -> ");
                current = current.next[i];
            }
            sb.append("END\n");
        }
        return sb.toString();
    }

    private static final class Node {

        ByteArrayPair val;
        Node[] next;

        Node(ByteArrayPair val, int numLevels) {
            this.val = val;
            this.next = new Node[numLevels];
        }

    }

    private static class SkipListIterator implements Iterator<ByteArrayPair> {

        Node node;

        SkipListIterator(Node node) {
            this.node = node;
        }

        @Override
        public boolean hasNext() {
            return node.next[0] != null;
        }

        @Override
        public ByteArrayPair next() {
            var res = node.next[0].val;
            node = node.next[0];

            return res;
        }

    }

}
