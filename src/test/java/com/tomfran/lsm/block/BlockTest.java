package com.tomfran.lsm.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockTest {

    Block b = new Block(12);

    @BeforeEach
    public void setup() {
        b.data.clear();
        b.offsets.clear();
    }

    @Test
    public void shouldAdd() {
        boolean res = b.add(new byte[]{1}, new byte[]{2});
        assert res;
    }

    @Test
    public void shouldFill() {
        b.add(new byte[]{1}, new byte[]{2});
        assert !b.add(new byte[]{1}, new byte[]{2});
    }

    @Test
    public void shouldNotAddKeyTooBig() {
        boolean res = b.add(new byte[Block.MAX_KEY_SIZE + 1], new byte[]{2});
        assert !res;
    }

    @Test
    public void shouldNotAddValueTooBig() {
        boolean res = b.add(new byte[]{1}, new byte[Block.MAX_VALUE_SIZE + 1]);
        assert !res;
    }

    @Test
    public void shouldPrint() {
        b.add(new byte[]{1}, new byte[]{2});
        b.add(new byte[]{2}, new byte[]{3});
        b.add(new byte[]{3}, new byte[]{4});
    }
}