package com.tomfran.lsm.bloom;


import com.tomfran.lsm.io.BaseInputStream;
import com.tomfran.lsm.io.BaseOutputStream;
import it.unimi.dsi.fastutil.longs.LongLongMutablePair;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import org.apache.commons.codec.digest.MurmurHash3;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * Bloom filter implementation.
 * <p>
 * Given the number of expected insertions and the desired false positive rate,
 * the size is computed as -expectedInsertions * log(falsePositiveRate) / (log(2) * log(2))
 * and the number of hash functions is computed as ceil(-log(falsePositiveRate) / log(2)).
 * <p>
 * Two hashes are computed for each key using a single MurmurHash3 128 bit call.
 * We then use the formula (h1 + i * h2) % size to get the ith hash for the key.
 */
public class BloomFilter {

    static final int DEFAULT_SIZE = 1 << 20;

    final int size;
    final int hashCount;
    final long[] bits;

    /**
     * Create a new Bloom filter with the default size and a false positive rate of 0.1%.
     */
    public BloomFilter() {
        this(DEFAULT_SIZE, 0.001);
    }


    /**
     * Create a new Bloom filter with the given expected insertions and a false positive rate of 0.1%.
     *
     * @param expectedInsertions The number of expected insertions.
     */
    public BloomFilter(int expectedInsertions) {
        this(expectedInsertions, 0.001);
    }


    /**
     * Create a new Bloom filter with the given expected insertions and false positive rate.
     *
     * @param expectedInsertions The number of expected insertions.
     * @param falsePositiveRate  The desired false positive rate.
     */
    public BloomFilter(int expectedInsertions, double falsePositiveRate) {
        this.size = (int) (-expectedInsertions * log(falsePositiveRate) / (log(2) * log(2)));
        this.hashCount = (int) ceil(-log(falsePositiveRate) / log(2));
        this.bits = new long[(int) ceil(size / 64.0)];
    }

    /**
     * Create a new Bloom filter from the given parameters.
     *
     * @param size      The size of the Bloom filter in bits.
     * @param hashCount The number of hash functions.
     * @param bits      The bits of the Bloom filter.
     */
    public BloomFilter(int size, int hashCount, long[] bits) {
        this.size = size;
        this.hashCount = hashCount;
        this.bits = bits;
    }

    /**
     * Read a Bloom filter from the given file.
     *
     * @param filename The file to read from.
     * @return The Bloom filter.
     */
    public static BloomFilter readFromFile(String filename) {
        BaseInputStream is = new BaseInputStream(filename);
        try {
            int size = is.readVByteInt();
            int hashCount = is.readVByteInt();
            int bitsLength = is.readVByteInt();
            long[] bits = new long[bitsLength];

            for (int i = 0; i < bitsLength; i++)
                bits[i] = is.readLong();

            is.close();
            return new BloomFilter(size, hashCount, bits);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a key to the Bloom filter.
     *
     * @param key The key to add.
     */
    public void add(byte[] key) {
        LongLongPair hash = getHash(key);
        long h1 = hash.leftLong(), h2 = hash.rightLong();

        for (int i = 0; i < hashCount; i++) {
            int bit = (int) Math.abs((h1 + i * h2) % size);
            bits[bit / 64] |= 1L << (bit % 64);
        }
    }

    /**
     * Check if the Bloom filter might contain the given key.
     *
     * @param key The key to check.
     * @return True if the Bloom filter might contain the key, false otherwise.
     */
    public boolean mightContain(byte[] key) {
        LongLongPair hash = getHash(key);
        long h1 = hash.leftLong(), h2 = hash.rightLong();

        for (int i = 0; i < hashCount; i++) {
            int bit = (int) Math.abs((h1 + i * h2) % size);
            if ((bits[bit / 64] & (1L << (bit % 64))) == 0)
                return false;
        }

        return true;
    }

    private LongLongMutablePair getHash(byte[] key) {
        long[] hashes = MurmurHash3.hash128x64(key, 0, key.length, 0);
        return LongLongMutablePair.of(hashes[0], hashes[1]);
    }

    /**
     * Write the Bloom filter to the given file.
     *
     * @param filename The file to write to.
     */
    public void writeToFile(String filename) {
        BaseOutputStream os = new BaseOutputStream(filename);

        os.writeVByteInt(size);
        os.writeVByteInt(hashCount);

        os.writeVByteInt(bits.length);

        for (var b : bits)
            os.writeLong(b);

        os.close();
    }

}
