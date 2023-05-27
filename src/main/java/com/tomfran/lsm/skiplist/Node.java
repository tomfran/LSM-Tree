package com.tomfran.lsm.skiplist;

import java.util.Arrays;

public class Node {

    protected final byte[] key;
    protected final Node[] next;
    protected byte[] value;

    public Node(byte[] key, byte[] value, Node[] next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }

    @Override
    public String toString() {
        return "(" + Arrays.toString(key) + ")";
    }
}