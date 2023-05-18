package com.tomfran.lsm.block;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class Block {

    static final int MAX_KEY_SIZE = 65536;
    static final int MAX_VALUE_SIZE = 65536;

    protected final ByteArrayList data;
    protected final ShortArrayList offsets;
    protected final int maxSize;

    public Block(int maxSize) {
        data = new ByteArrayList();
        offsets = new ShortArrayList();
        this.maxSize = maxSize;
    }

    public boolean add(byte[] key, byte[] value) {
        int requiredSpace = key.length + value.length + 6;
        int currentSpace = data.size() + offsets.size() * 2;

        if ((requiredSpace + currentSpace) > maxSize || key.length > MAX_KEY_SIZE || value.length > MAX_VALUE_SIZE)
            return false;

        // write current offset cast is safe because we know the max size of the array
        offsets.add((short) data.size());

        // add key
        data.addElements(data.size(), getByteArray(key.length));
        data.addElements(data.size(), key);

        // add value
        data.addElements(data.size(), getByteArray(value.length));
        data.addElements(data.size(), value);

        return true;
    }

    private byte[] getByteArray(int x) {
        return new byte[]{(byte) (x >> 8), (byte) x};
    }

    public int getSize() {
        return data.size() + offsets.size() * 2;
    }

}
