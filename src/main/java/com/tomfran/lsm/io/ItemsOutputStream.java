package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.FileOutputStream;

public class ItemsOutputStream {

    private static final byte[] VBYTE_BUFFER = new byte[5];
    private final FastBufferedOutputStream fos;

    public ItemsOutputStream(String filename) {
        try {
            fos = new FastBufferedOutputStream(new FileOutputStream(filename));
            fos.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] intToVByte(int n) {
        int i = 0;
        while (n > 0) {
            VBYTE_BUFFER[i++] = (byte) (n & 0x7F);
            n >>>= 7;
        }

        VBYTE_BUFFER[i - 1] |= 0x80;
        byte[] res = new byte[i];
        System.arraycopy(VBYTE_BUFFER, 0, res, 0, i);
        return res;
    }

    public int writeItem(Item item) {
        try {
            byte[] bytes = toBytes(item);
            fos.write(bytes);
            return bytes.length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
