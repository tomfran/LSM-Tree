package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;

public class ItemsInputStream extends BaseInputStream {

    public ItemsInputStream(String filename) {
        super(filename);
    }

    public Item readItem() {
        try {
            int keyLength = readVByteInt();
            int valueLength = readVByteInt();

            return new Item(
                    readNBytes(keyLength),
                    readNBytes(valueLength)
            );
        } catch (Exception e) {
            return null;
        }
    }

}
