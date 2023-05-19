package com.tomfran.lsm.table;

import com.tomfran.lsm.block.Block;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import static java.util.Arrays.compare;

public class SSTable {

    protected int blockSize;
    protected ObjectArrayList<Block> blocks;
    protected ObjectArrayList<byte[]> firstKeys;
    protected IntArrayList offsets;

    public SSTable(int blockSize) {
        this.blockSize = blockSize;
        blocks = new ObjectArrayList<>();
        firstKeys = new ObjectArrayList<>();
        offsets = new IntArrayList();
    }

    public void add(byte[] key, byte[] value) {
        Block last = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);

        if (last == null || !last.add(key, value)) {
            last = addNewBlock();
            firstKeys.add(key);
        }

        last.add(key, value);
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

    public int getCandidateBlock(byte[] key) {
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
        if (compare(key, firstKeys.get(j)) <= 0)
            return i;
        else
            return j;
    }

}
