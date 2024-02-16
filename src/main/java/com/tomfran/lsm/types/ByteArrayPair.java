package com.tomfran.lsm.types;

import java.util.Arrays;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public record ByteArrayPair(byte[] key, byte[] value) implements Comparable<ByteArrayPair> {

    public int size() {
        return key.length + value.length;
    }

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
