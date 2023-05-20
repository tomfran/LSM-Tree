package com.tomfran.lsm.sstable;

import com.tomfran.lsm.block.BlockIterator;
import com.tomfran.lsm.interfaces.Iterator;

public class SSTableIterator implements Iterator {

    private final SSTable table;
    private final int maxBlockIndex;
    private int blockIndex;

    private BlockIterator currentIterator;

    public SSTableIterator(SSTable table) {
        this.table = table;
        this.blockIndex = 0;
        this.maxBlockIndex = table.blocks.size() - 1;
        if (maxBlockIndex < 0)
            throw new RuntimeException("No blocks in table");

        currentIterator = new BlockIterator(table.blocks.get(0));
    }

    public boolean hasNext() {
        return blockIndex < maxBlockIndex || currentIterator.hasNext();
    }

    public void next() {
        if (currentIterator.hasNext()) {
            currentIterator.next();
        } else {
            blockIndex++;
            currentIterator = new BlockIterator(table.blocks.get(blockIndex));
            currentIterator.next();
        }
    }

    public byte[] key() {
        return currentIterator.key();
    }

    public byte[] value() {
        return currentIterator.value();
    }

}
