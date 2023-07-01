package com.tomfran.lsm.sstable;

import com.google.common.hash.HashFunction;
import com.google.common.primitives.Longs;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import static com.google.common.hash.Hashing.murmur3_128;

public class BloomFilterCollisionBenchmark {

    static long numBits;
    static int numHashFunctions;
    static HashFunction h = murmur3_128();
    static Long2LongOpenHashMap map = new Long2LongOpenHashMap();

    public static void main(String[] args) {

        int n = 10000;
        numHashFunctions = (int) Math.ceil(Math.log(n) / Math.log(2));
        numBits = (long) Math.ceil(n * numHashFunctions / Math.log(2));

        for (int i = 0; i < n; i++)
            add(randomBytes());

        var values = map.values();
        double avg = values.longStream().average().getAsDouble();
        long max = values.longStream().max().getAsLong();
        long min = values.longStream().min().getAsLong();
        long median = values.longStream().sorted().skip(map.size() / 2).findFirst().getAsLong();

        System.out.printf("avg: %f, max: %d, min: %d, median: %d\n", avg, max, min, median);
    }

    public static byte[] randomBytes() {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (Math.random() * 256);
        bytes[1] = (byte) (Math.random() * 256);
        bytes[2] = (byte) (Math.random() * 256);
        bytes[3] = (byte) (Math.random() * 256);
        return bytes;
    }

    public static void add(byte[] e) {
        byte[] hash = h.hashBytes(e).asBytes();

        long h1 = Longs.fromBytes(hash[0], hash[1], hash[2], hash[3], hash[4], hash[5], hash[6], hash[7]);
        long h2 = Longs.fromBytes(hash[8], hash[9], hash[10], hash[11], hash[12], hash[13], hash[14], hash[15]);

        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = h1 + i * h2;
            long index = Math.floorMod(combinedHash, numBits);
            map.put(index, map.getOrDefault(index, 0L) + 1);
        }
    }
}
