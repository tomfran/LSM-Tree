package com.tomfran.lsm.sstable;

import java.util.function.Function;

public class BloomFilterBenchmark {

    public static final int N = 1000;
    public static BloomFilter b = new BloomFilter(N);

    public static void main(String[] args) {

        Function<Integer, byte[]> f = i -> {
            var bytes = new byte[4];
            bytes[0] = (byte) (i & 0xFF);
            bytes[1] = (byte) ((i >> 8) & 0xFF);
            bytes[2] = (byte) ((i >> 16) & 0xFF);
            bytes[3] = (byte) ((i >> 24) & 0xFF);
            return bytes;
        };

        for (int i = 0; i < N; i++)
            b.add(f.apply(i));

        int falsePositives = 0;

        for (int i = N; i < 2 * N; i++) {
            if (b.contains(f.apply(i)))
                falsePositives++;
        }

        System.out.println("False positives rate for " + N + " entries: " + (double) falsePositives / N);
        System.out.println("MB required for " + N + " entries: " + (double) b.getNumBits() / 1024 / 1024 + "MB");
    }
}
