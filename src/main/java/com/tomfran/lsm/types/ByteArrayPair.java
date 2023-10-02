package com.tomfran.lsm.types;

import java.util.Arrays;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public record ByteArrayPair(byte[] key, byte[] value) implements Comparable<ByteArrayPair> {

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public int compareTo(ByteArrayPair o) {
        return compare(key, o.key);
    }

    @Override
    public String toString() {
        // binary representation of key and value, e.g. (1010101, 010101010)
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (byte b : key) {
            sb.append(b);
        }
        sb.append(", ");
        for (byte b : value) {
            sb.append(b);
        }
        sb.append(")");
        return sb.toString();
    }
}
