package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import java.io.FileInputStream;
import java.io.IOException;

public class ItemsInputStream {

    private final FastBufferedInputStream fis;

    public ItemsInputStream(String filename) {
        try {
            fis = new FastBufferedInputStream(new FileInputStream(filename));
            fis.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Item readItem() {
        try {
            int keyLength = readVByteInt();
            int valueLength = readVByteInt();

            return new Item(
                    fis.readNBytes(keyLength),
                    fis.readNBytes(valueLength)
            );
        } catch (Exception e) {
            return null;
        }
    }

    protected int readVByteInt() throws IOException {
        int result = 0;
        byte[] b;
        int shift = 0;
        while (true) {
            b = fis.readNBytes(1);
            result |= (((int) b[0] & 0x7F) << shift);

            if ((b[0] & 0x80) == 0x80) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    public void close() {
        try {
            fis.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void seek(long offset) {
        try {
            fis.position(offset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasNext() {
        try {
            return fis.available() > 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
