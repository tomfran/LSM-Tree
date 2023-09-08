package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;

public class ItemsOutputStream extends BaseOutputStream {

    public ItemsOutputStream(String filename) {
        super(filename);
    }

    public int writeItem(Item item) {
        return write(toBytes(item));
    }

    protected byte[] toBytes(Item i) {
        byte[] key = i.key(), value = i.value();
        byte[] keyBytes = intToVByte(key.length), valueBytes = intToVByte(value.length);

        byte[] result = new byte[keyBytes.length + valueBytes.length + key.length + value.length];

        System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
        System.arraycopy(valueBytes, 0, result, keyBytes.length, valueBytes.length);

        System.arraycopy(key, 0, result, keyBytes.length + valueBytes.length, key.length);
        System.arraycopy(value, 0, result, keyBytes.length + valueBytes.length + key.length, value.length);

        return result;
    }
}
