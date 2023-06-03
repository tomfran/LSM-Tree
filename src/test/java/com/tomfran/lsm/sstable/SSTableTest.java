package com.tomfran.lsm.sstable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class SSTableTest {

    SSTable t = new SSTable(128);

    @BeforeEach
    public void setup() {
        t.blocks.clear();
        t.firstKeys.clear();
        t.offsets.clear();
    }

    @Test
    public void shouldAdd() {
        t.put(new byte[]{1}, new byte[]{2});
        assert t.blocks.size() == 1;
    }

    @Test
    public void shouldGet() {
        IntStream.range(0, 100).forEach(i -> t.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)}));

        for (int i = 0; i < 100; i++) {
            var key = new byte[]{(byte) i};
            var value = t.get(key);

            assert value != null;
            assert value[0] == (byte) (i + 1);
        }
    }
}