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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Item readItem() {
        try {
            int keyLength = readInt();
            int valueLength = readInt();

            return new Item(
                    fis.readNBytes(keyLength),
                    fis.readNBytes(valueLength)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public int readInt() throws IOException {
        byte[] buffer = fis.readNBytes(4);
        return (buffer[0] & 0xFF) << 24 |
                (buffer[1] & 0xFF) << 16 |
                (buffer[2] & 0xFF) << 8 |
                (buffer[3] & 0xFF);
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
