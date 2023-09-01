package com.tomfran.lsm.bloom;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;

import static com.tomfran.lsm.TestUtils.getRandomByteArrayList;

class BloomFilterTest {

    static final int INSERTIONS = 10000;
    static final double FALSE_POSITIVE_RATE = 0.01;
    static BloomFilter b;
    static DoubleArrayList results = new DoubleArrayList();

    @AfterAll
    static void tearDown() {
        double avg = results.doubleStream().average().orElse(FALSE_POSITIVE_RATE * 100);

        assert avg < FALSE_POSITIVE_RATE * 1.1 && avg > FALSE_POSITIVE_RATE * 0.9 :
                "False positive rate is not close to the expected value: " + avg;
    }

    @RepeatedTest(10)
    void testAdd() {
        b = new BloomFilter(INSERTIONS, FALSE_POSITIVE_RATE);

        var data = getRandomByteArrayList(2 * INSERTIONS);
        data.stream().skip(INSERTIONS).forEach(d -> {
            b.add(d);
            assert b.mightContain(d);
        });

        int falsePositives = data.stream().limit(INSERTIONS)
                .mapToInt(d -> b.mightContain(d) ? 1 : 0).sum();

        double falsePositiveRate = falsePositives / (double) INSERTIONS;
        results.add(falsePositiveRate);
    }
}