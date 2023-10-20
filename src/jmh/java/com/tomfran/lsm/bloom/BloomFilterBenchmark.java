package com.tomfran.lsm.bloom;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomByteArray;

@OutputTimeUnit(TimeUnit.SECONDS)
public class BloomFilterBenchmark {

    @Benchmark
    public void add(BloomState s) {
        s.f.add(s.keys[s.index]);

        s.index = (s.index + 1) % BloomState.N;
    }

    @Benchmark
    public void contains(BloomState s, Blackhole bh) {
        bh.consume(s.f.mightContain(s.keys[s.index]));

        s.index = (s.index + 1) % BloomState.N;
    }

    @State(Scope.Thread)
    public static class BloomState {

        static final int N = 1000000;

        BloomFilter f;
        byte[][] keys = new byte[N][];
        int index;

        @Setup
        public void setup() {
            f = new BloomFilter(N);
            index = 0;
            for (int i = 0; i < N; i++)
                keys[i] = getRandomByteArray();

            for (int i = 0; i < N / 2; i++)
                f.add(keys[i]);
        }

    }

}
