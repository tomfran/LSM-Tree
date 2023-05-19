package com.tomfran.lsm.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class SSTableTest {

    SSTable t = new SSTable(12);

    @BeforeEach
    public void setup() {
        t.blocks.clear();
        t.firstKeys.clear();
        t.offsets.clear();
    }

    @Test
    public void shouldAdd() {
        t.add(new byte[]{1}, new byte[]{2});
        assert t.blocks.size() == 1;
    }

    @Test
    public void shouldAddTwoBlocks() {
        t.add(new byte[]{1}, new byte[]{2});
        t.add(new byte[]{2}, new byte[]{3});
        assert t.blocks.size() == 2;
    }

    @Test
    public void shouldFindCandidate() {
        IntStream.range(0, 10).forEach(i -> t.firstKeys.add(new byte[]{(byte) i}));

        assert t.getCandidateBlock(new byte[]{5}) == 4;
        assert t.getCandidateBlock(new byte[]{10}) == 9;
        assert t.getCandidateBlock(new byte[]{0}) == 0;
        assert t.getCandidateBlock(new byte[]{100}) == 9;
    }
}