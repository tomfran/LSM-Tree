package com.tomfran.lsm.tree;

import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.BenchmarkUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.utils.BenchmarkUtils.shuffleItems;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class LSMTreeGetBenchmark {

    static final Path DIR = Path.of("tree_benchmark");
    static final int NUM_ITEMS = 1000000;
    static final int MEMTABLE_SIZE = 1 << 18;
    static final int LEVEL_SIZE = 5;
    static int index = 0;

    LSMTree tree;
    ByteArrayPair[] items;

    @Setup
    public void setup() throws IOException {
        tree = BenchmarkUtils.initTree(DIR, MEMTABLE_SIZE, LEVEL_SIZE);
        items = BenchmarkUtils.fillItems(NUM_ITEMS);
        for (var i : items)
            tree.add(i);

        shuffleItems(items);
    }

    @TearDown
    public void teardown() throws IOException {
        tree.stop();
        BenchmarkUtils.deleteDir(DIR);
    }

    @Benchmark
    public void get(Blackhole bh) {
        var item = items[index].key();
        bh.consume(tree.get(item));

        index = (index + 1) % NUM_ITEMS;
    }

}
