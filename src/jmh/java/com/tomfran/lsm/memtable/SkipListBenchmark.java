package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomPair;

@OutputTimeUnit(TimeUnit.SECONDS)
public class SkipListBenchmark {

    @Benchmark
    public void addRemove(ListState s, Blackhole bh) {
        var key = s.items[s.index].key();

        if (s.addRemove[s.index]) {
            s.l.add(s.items[s.index]);
        } else {
            s.l.remove(key);
        }

        s.index = (s.index + 1) % ListState.N;
    }

    @Benchmark
    public void get(ListState s, Blackhole bh) {
        var key = s.items[s.index].key();
        var found = s.l.get(key);

        bh.consume(found);

        s.index = (s.index + 1) % ListState.N;
    }

    @State(Scope.Thread)
    public static class ListState {

        static final int N = 200000;

        SkipList l;
        ByteArrayPair[] items;
        int index;
        boolean[] addRemove;

        @Setup
        public void setup() {
            l = new SkipList(N / 2);

            ObjectArrayList<ByteArrayPair> tmp = new ObjectArrayList<>();
            for (int i = 0; i < N; i++) {
                var it = getRandomPair();
                if (i < N / 2)
                    l.add(it);

                tmp.add(it);
            }

            items = tmp.toArray(new ByteArrayPair[0]);

            index = 0;

            addRemove = new boolean[N];
            for (int i = 0; i < N; i++) {
                addRemove[i] = Math.random() < 0.5;
            }
        }

    }

}
