package com.tomfran.lsm.sstable;

import com.google.common.hash.HashFunction;
import com.google.common.primitives.Longs;

import static com.google.common.hash.Hashing.murmur3_128;

public class BloomFilter {

    private final long numBits;
    private final long[] bits;
    private final int numHashFunctions;
    private final HashFunction h = murmur3_128();

    public BloomFilter(int n) {
        numHashFunctions = (int) Math.ceil(Math.log(n) / Math.log(2));
        numBits = (long) Math.ceil(n * numHashFunctions / Math.log(2));
        bits = new long[(int) Math.ceil(numBits / 64.0)];
    }

    public void add(byte[] e) {
        byte[] hash = h.hashBytes(e).asBytes();

        long h1 = Longs.fromBytes(hash[0], hash[1], hash[2], hash[3], hash[4], hash[5], hash[6], hash[7]);
        long h2 = Longs.fromBytes(hash[8], hash[9], hash[10], hash[11], hash[12], hash[13], hash[14], hash[15]);

        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = h1 + i * h2;
            long index = Math.floorMod(combinedHash, numBits);
            bits[(int) (index / 64)] |= 1L << (index % 64);
        }
    }

    public boolean contains(byte[] e) {
        byte[] hash = h.hashBytes(e).asBytes();

        long h1 = Longs.fromBytes(hash[0], hash[1], hash[2], hash[3], hash[4], hash[5], hash[6], hash[7]);
        long h2 = Longs.fromBytes(hash[8], hash[9], hash[10], hash[11], hash[12], hash[13], hash[14], hash[15]);

        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = h1 + i * h2;
            long index = Math.floorMod(combinedHash, numBits);
            if ((bits[(int) (index / 64)] & (1L << (index % 64))) == 0)
                return false;
        }
        return true;
    }

    public long getNumBits() {
        return numBits;
    }
}
