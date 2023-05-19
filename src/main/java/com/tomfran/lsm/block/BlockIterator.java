package com.tomfran.lsm.block;

public class BlockIterator {

    private final Block block;
    private final int maxIndex;
    private int index;

    public BlockIterator(Block block) {
        this.block = block;
        this.index = 0;
        this.maxIndex = block.offsets.size() - 1;
    }

    public boolean hasNext() {
        return index < maxIndex;
    }

    public void next() {
        index++;
    }

    public byte[] key() {
        short currentOffset = block.offsets.getShort(index);
        short keyLength = readShort(currentOffset);

        byte[] key = new byte[keyLength];
        for (int i = 0; i < keyLength; i++)
            key[i] = block.data.getByte(currentOffset + 2 + i);

        return key;
    }

    public byte[] value() {
        short currentOffset = block.offsets.getShort(index);
        short keyLength = readShort(currentOffset);
        short valueLength = readShort(currentOffset + 2 + keyLength);

        byte[] value = new byte[valueLength];
        for (int i = 0; i < valueLength; i++)
            value[i] = block.data.getByte(currentOffset + 4 + keyLength + i);

        return value;
    }

    public short readShort(int i) {
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
