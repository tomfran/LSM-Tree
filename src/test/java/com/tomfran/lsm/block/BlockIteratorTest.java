package com.tomfran.lsm.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockIteratorTest {

    Block b = new Block(32);
    BlockIterator it;

    @BeforeEach
    public void setup() {
        b.data.clear();
        b.offsets.clear();

        b.add(new byte[]{1, 2}, new byte[]{2});
        b.add(new byte[]{2}, new byte[]{3});
        b.add(new byte[]{3}, new byte[]{4});

        it = new BlockIterator(b);
    }

    @Test
    public void shouldGetEntries() {
        assert it.key()[0] == 1;
        assert it.key()[1] == 2;
        assert it.value()[0] == 2;

        it.next();
        assert it.key()[0] == 2;
        assert it.value()[0] == 3;

        it.next();
        assert it.key()[0] == 3;
        assert it.value()[0] == 4;

        assert !it.hasNext();
    }

    @Test
    public void shouldReadLongs() {
        byte[] k = it.key();
        assert it.readLong(k) == 258;
    }
    
}