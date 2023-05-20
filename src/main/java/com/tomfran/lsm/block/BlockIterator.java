package com.tomfran.lsm.block;

import com.tomfran.lsm.interfaces.Iterator;

public class BlockIterator implements Iterator {

    protected final Block block;
    protected final int maxIndex;
    protected int index;

    public BlockIterator(Block block) {
        this.block = block;
        this.index = -1;
        this.maxIndex = block.offsets.size() - 1;
    }

    public boolean hasNext() {
        return index < maxIndex;
    }

    public void next() {
        index++;
    }

    public byte[] key() {
        if (index < 0 || index > maxIndex)
            throw new RuntimeException("Invalid element index: " + index);

        short currentOffset = block.offsets.getShort(index);
        short keyLength = readShort(currentOffset);

        byte[] key = new byte[keyLength];
        for (int i = 0; i < keyLength; i++)
            key[i] = block.data.getByte(currentOffset + 2 + i);

        return key;
    }

    public byte[] value() {
        if (index < 0 || index > maxIndex)
            throw new RuntimeException("Invalid element index: " + index);

        short currentOffset = block.offsets.getShort(index);
        short keyLength = readShort(currentOffset);
        short valueLength = readShort(currentOffset + 2 + keyLength);

        byte[] value = new byte[valueLength];
        for (int i = 0; i < valueLength; i++)
            value[i] = block.data.getByte(currentOffset + 4 + keyLength + i);

        return value;
    }

    private short readShort(int i) {
        return (short) ((short) block.data.getByte(i) << 4 | (short) block.data.getByte(i + 1));
    }

    public long readLong(byte[] arr) {
        long result = 0;
        for (int i = 0; i < arr.length; i++) {
            result <<= 8;
            result |= arr[i];
        }

        return result;
    }

}
