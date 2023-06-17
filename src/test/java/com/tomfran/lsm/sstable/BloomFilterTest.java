package com.tomfran.lsm.sstable;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

class BloomFilterTest {

    @Test
    void bloomTest() {
        int n = 100;

        var b = new BloomFilter(n);

        Function<Integer, byte[]> f = i -> {
            var bytes = new byte[4];
            bytes[0] = (byte) (i & 0xFF);
            bytes[1] = (byte) ((i >> 8) & 0xFF);
            bytes[2] = (byte) ((i >> 16) & 0xFF);
            bytes[3] = (byte) ((i >> 24) & 0xFF);
            return bytes;
        };

        for (int i = 0; i < n; i++)
            b.add(f.apply(i));

        for (int i = 0; i < n; i++)
            assert b.contains(f.apply(i));
    }

}