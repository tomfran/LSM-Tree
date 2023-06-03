package com.tomfran.lsm.sstable;

import com.tomfran.lsm.block.Block;
import com.tomfran.lsm.iterator.Iterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import static java.util.Arrays.compare;

public class SSTable {

    static final int DEFAULT_BLOCK_SIZE = 1024;

    protected int blockSize;
    protected ObjectArrayList<Block> blocks;
    protected ObjectArrayList<byte[]> firstKeys;
    protected IntArrayList offsets;

    public SSTable() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public SSTable(int blockSize) {
        this.blockSize = blockSize;
        blocks = new ObjectArrayList<>();
        firstKeys = new ObjectArrayList<>();
        offsets = new IntArrayList();
    }

    public void put(byte[] key, byte[] value) {
        Block last = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);

        if (last == null || !last.add(key, value)) {
            last = addNewBlock();
            firstKeys.add(key);
            last.add(key, value);
        }
    }

    public byte[] get(byte[] key) {
        if (mightContain(key))
            return search(key);

        return null;
    }

    private boolean mightContain(byte[] key) {
        return true;
    }

    private byte[] search(byte[] key) {
        int blockIndex = getCandidateBlock(key);
        if (blockIndex < 0)
            return null;

        Block block = blocks.get(blockIndex);
        Iterator it = block.iterator();

        while (it.hasNext()) {
            it.next();
            if (compare(key, it.key()) == 0)
                return it.value();
        }

        return null;
    }

    private Block addNewBlock() {
        addOffset();

        Block block = new Block(blockSize);
        blocks.add(block);
        return block;
    }

    private void addOffset() {
        int previousOffset = offsets.isEmpty() ? 0 : offsets.getInt(offsets.size() - 1);
        int lastBlockSize = blocks.isEmpty() ? 0 : blocks.get(blocks.size() - 1).getSize();
        offsets.add(previousOffset + lastBlockSize);
    }

    private int getCandidateBlock(byte[] key) {
        int i, j, m, comp;
        i = 0;
        j = firstKeys.size() - 1;
        while (i < (j - 1)) {
            m = (i + j) / 2;

            comp = compare(key, firstKeys.get(m));
            if (comp == 0)
                return m;
            else if (comp < 0)
                j = m - 1;
            else
                i = m;
        }
        if (compare(key, firstKeys.get(j)) < 0)
            return i;
        else
            return j;
    }

    public SSTableIterator iterator() {
        return new SSTableIterator(this);
    }

}
