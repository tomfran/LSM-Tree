package com.tomfran.lsm.tree;

import com.tomfran.lsm.types.ByteArrayPair;
import com.tomfran.lsm.utils.BenchmarkUtils;
import org.openjdk.jmh.annotations.*;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class LSMTreeAddBenchmark {

    static final Path DIR = Path.of("tree_add_benchmark");
    static final int NUM_ITEMS = 1000000;
    static final int MEMTABLE_SIZE = 1024 * 1024 * 256;
    static final int LEVEL_SIZE = 4;
    static int index = 0;

    LSMTree tree;
    ByteArrayPair[] items;

    @Setup
    public void setup() {
        tree = BenchmarkUtils.initTree(DIR, MEMTABLE_SIZE, LEVEL_SIZE);
        items = BenchmarkUtils.fillItems(NUM_ITEMS);
    }

    @TearDown
    public void teardown() {
        BenchmarkUtils.stopTreeAndCleanDisk(tree, DIR);
    }

    @Benchmark
    public void add() {
        var item = items[index];
        tree.add(item);

        index = (index + 1) % NUM_ITEMS;
    }

}
