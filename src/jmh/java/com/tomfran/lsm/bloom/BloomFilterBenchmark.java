package com.tomfran.lsm.bloom;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomByteArray;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class BloomFilterBenchmark {

    BloomFilter bf;

    byte[][] keys;

    int N = 1000000;
    int index = 0;

    @Setup
    public void setup() {

        bf = new BloomFilter(N, 0.01);

        keys = new byte[N][];

        for (int i = 0; i < N; i++)
            keys[i] = getRandomByteArray();
    }

    @Benchmark
    public void add() {
        bf.add(keys[index]);

        index = (index + 1) % N;
    }

    @Benchmark
    public void contains(Blackhole bh) {
        bh.consume(bf.mightContain(keys[index]));

        index = (index + 1) % N;
    }

}
