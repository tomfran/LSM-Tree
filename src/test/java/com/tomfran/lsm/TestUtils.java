package com.tomfran.lsm;

import com.tomfran.lsm.types.ByteArrayPair;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class TestUtils {

    static final int MIN_BYTES_LENGTH = 10, MAX_BYTES_LENGTH = 500;
    static Random rn = new Random();

    public static ByteArrayPair getRandomPair() {
        return new ByteArrayPair(
                getRandomByteArray(),
                getRandomByteArray()
        );
    }

    public static byte[] getRandomByteArray(int length) {
        byte[] bytes = new byte[length];
        rn.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getRandomByteArray() {
        byte[] bytes = new byte[rn.nextInt(MIN_BYTES_LENGTH, MAX_BYTES_LENGTH)];
        rn.nextBytes(bytes);
        return bytes;
    }

    public static List<byte[]> getRandomByteArrayList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> getRandomByteArray())
                .toList();
    }

    public static void assertPairEqual(ByteArrayPair a, ByteArrayPair b) {
        assert compare(a.key(), b.key()) == 0;
        assert compare(a.value(), b.value()) == 0;
    }
}
