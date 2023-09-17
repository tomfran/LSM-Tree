package com.tomfran.lsm.memtable;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomItem;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SkipListBenchmark {

    SkipList l;
    Item[] items;

    int NUM_ITEMS = 200000;
    int index = 0;

    boolean[] addRemove;

    @Setup
    public void setup() {

        l = new SkipList(NUM_ITEMS / 2);

        // generate random items and insert half
        ObjectArrayList<Item> tmp = new ObjectArrayList<>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            var it = getRandomItem();
            if (i < NUM_ITEMS / 2)
                l.add(it);

            tmp.add(it);
        }

        items = tmp.toArray(new Item[0]);

        // generate sequence of add/remove operations
        addRemove = new boolean[NUM_ITEMS];
        for (int i = 0; i < NUM_ITEMS; i++) {
            addRemove[i] = Math.random() < 0.5;
        }
    }

    @Benchmark
    public void get(Blackhole bh) {
        var key = items[index].key();
        var found = l.get(key);

        bh.consume(found);

        index = (index + 1) % NUM_ITEMS;
    }

    @Benchmark
    public void addRemove(Blackhole bh) {
        var key = items[index].key();

        if (addRemove[index]) {
            l.add(items[index]);
        } else {
            l.remove(key);
        }

        index = (index + 1) % NUM_ITEMS;
    }

}
