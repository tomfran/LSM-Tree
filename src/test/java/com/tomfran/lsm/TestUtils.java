package com.tomfran.lsm;

import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.types.Item;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class TestUtils {

    static final int MIN_BYTES_LENGTH = 10, MAX_BYTES_LENGTH = 500;
    Random rn = new Random();

    public static Item getRandomItem() {
        return new Item(
                getRandomByteArray(),
                getRandomByteArray()
        );
    }

    static byte[] getRandomByteArray() {
        Random rn = new Random();
        byte[] bytes = new byte[rn.nextInt(MIN_BYTES_LENGTH, MAX_BYTES_LENGTH)];
        rn.nextBytes(bytes);
        return bytes;
    }

    public static List<byte[]> getRandomByteArrayList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> getRandomByteArray())
                .toList();
    }

    public static void assertItemEquals(Item a, Item b) {
        assert compare(a.key(), b.key()) == 0;
        assert compare(a.value(), b.value()) == 0;
    }

    public static void deleteFile(String filename) {
        new File(filename).delete();
    }

    public static void deleteSSTableFiles(String baseFilename) {
        deleteFile(baseFilename + SSTable.DATA_FILE_EXTENSION);
        deleteFile(baseFilename + SSTable.BLOOM_FILE_EXTENSION);
        deleteFile(baseFilename + SSTable.INDEX_FILE_EXTENSION);
    }
}
