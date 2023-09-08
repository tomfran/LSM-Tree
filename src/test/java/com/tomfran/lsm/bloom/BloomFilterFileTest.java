package com.tomfran.lsm.bloom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static com.tomfran.lsm.TestUtils.deleteFile;
import static com.tomfran.lsm.TestUtils.getRandomByteArrayList;

public class BloomFilterFileTest {

    static final String FILENAME = "bloom.test";

    @AfterAll
    static void tearDown() {
        deleteFile(FILENAME);
    }

    @Test
    public void shouldReconstruct() {
        BloomFilter bf = new BloomFilter(10000, 0.01);
        getRandomByteArrayList(10000).forEach(bf::add);
        bf.writeToFile(FILENAME);

        BloomFilter bf2 = BloomFilter.readFromFile(FILENAME);

        assert bf2.size == bf.size;
        assert bf2.hashCount == bf.hashCount;
        assert bf2.bits.length == bf.bits.length;

        for (int i = 0; i < bf.bits.length; i++) {
            assert bf.bits[i] == bf2.bits[i];
        }
    }
}
