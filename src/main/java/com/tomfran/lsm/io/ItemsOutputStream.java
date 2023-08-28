package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.FileOutputStream;

public class ItemsOutputStream {

    private final FastBufferedOutputStream fos;

    public ItemsOutputStream(String filename) {
        try {
            fos = new FastBufferedOutputStream(new FileOutputStream(filename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int writeItem(Item item) {
        try {
            byte[] bytes = item.toBytes();
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

}
