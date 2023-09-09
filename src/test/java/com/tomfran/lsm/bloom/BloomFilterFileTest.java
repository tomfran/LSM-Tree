package com.tomfran.lsm.bloom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.tomfran.lsm.TestUtils.getRandomByteArrayList;

public class BloomFilterFileTest {

    static final String FILENAME = "/filter.bloom";
    @TempDir
    static Path tempDirectory;

    @Test
    public void shouldReconstruct() {
        BloomFilter bf = new BloomFilter(10000, 0.01);
        getRandomByteArrayList(10000).forEach(bf::add);
        bf.writeToFile(tempDirectory + FILENAME);

        BloomFilter bf2 = BloomFilter.readFromFile(tempDirectory + FILENAME);

        assert bf2.size == bf.size;
        assert bf2.hashCount == bf.hashCount;
        assert bf2.bits.length == bf.bits.length;

        for (int i = 0; i < bf.bits.length; i++) {
            assert bf.bits[i] == bf2.bits[i];
        }
    }
}
