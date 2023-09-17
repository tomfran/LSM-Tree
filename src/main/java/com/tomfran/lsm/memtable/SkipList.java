package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import java.util.Iterator;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;
import static java.lang.Math.ceil;
import static java.lang.Math.log;

public class SkipList implements Iterable<Item> {

    static final int DEFAULT_ELEMENTS = 1 << 16;
    final Node sentinel;
    private final Node[] buffer;
    private final XoRoShiRo128PlusRandom rn;
    int levels;
    int size;

    public SkipList() {
        this(DEFAULT_ELEMENTS);
    }

    public SkipList(int numElements) {
        levels = (int) ceil(log(numElements) / log(2));
        size = 0;
        sentinel = new Node(null, levels);
        rn = new XoRoShiRo128PlusRandom();
        buffer = new Node[levels];
    }

    public void add(Item item) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && current.next[i].value.compareTo(item) < 0)
                current = current.next[i];
            buffer[i] = current;
        }

        if (current.next[0] != null && current.next[0].value.compareTo(item) == 0) {
            current.next[0].value = item;
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
        while (rn.nextBoolean() && level < levels)
            level++;
        return level;
    }

    public Item get(byte[] key) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && compare(current.next[i].value.key(), key) < 0)
                current = current.next[i];
        }

        if (current.next[0] != null && compare(current.next[0].value.key(), key) == 0)
            return current.next[0].value;

        return null;
    }

    public void remove(byte[] key) {
        Node current = sentinel;
        for (int i = levels - 1; i >= 0; i--) {
            while (current.next[i] != null && compare(current.next[i].value.key(), key) < 0)
                current = current.next[i];
            buffer[i] = current;
        }

        if (current.next[0] != null && compare(current.next[0].value.key(), key) == 0) {
            boolean last = current.next[0].next[0] == null;
            for (int i = 0; i < levels; i++) {
                if (buffer[i].next[i] != current.next[0])
                    break;
                buffer[i].next[i] = last ? null : current.next[0].next[i];
            }
            size--;
        }
    }

    public int size() {
        return size;
    }

    @Override
    public Iterator<Item> iterator() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = levels - 1; i >= 0; i--) {
            sb.append(String.format("Level %2d: ", i));
            Node current = sentinel;
            while (current.next[i] != null) {
                sb.append(current.next[i].value).append(" -> ");
                current = current.next[i];
            }
            sb.append("END\n");
        }
        return sb.toString();
    }

    static final class Node {
        Item value;
        Node[] next;

        Node(Item value, int numLevels) {
            this.value = value;
            this.next = new Node[numLevels];
        }
    }

}
