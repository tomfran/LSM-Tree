package com.tomfran.lsm.types;

import java.util.Arrays;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public record Item(byte[] key, byte[] value) implements Comparable<Item> {

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public int compareTo(Item o) {
        return compare(key, o.key);
    }
}
