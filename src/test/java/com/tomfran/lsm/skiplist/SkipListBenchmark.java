package com.tomfran.lsm.skiplist;

import java.util.random.RandomGenerator;

public class SkipListBenchmark {

    static RandomGenerator rn = RandomGenerator.of("Xoroshiro128PlusPlus");

    public static void main(String[] args) {

        int warmup = 100000;
        int size = 100000;

        int level = (int) (Math.log(size) / Math.log(2));
        SkipList map = new SkipList(level);

        for (int i = 0; i < warmup; i++) {
            map.add(f(), f());
        }

        long start = System.nanoTime();
        for (int i = 0; i < size; i++) {
            map.add(f(), f());
        }

        long end = System.nanoTime();
        System.out.println("Total time: " + (end - start) + " nanoseconds, " + size + " inserts");
        System.out.println("Nanoseconds per insert: " + (end - start) / size);

    }

    private static byte[] f() {
        int len = rn.nextInt(1, 10);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) rn.nextInt(0, 127);
        }
        return bytes;
    }
}
