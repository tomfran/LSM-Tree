package com.tomfran.lsm.tree;

import com.tomfran.lsm.types.ByteArrayPair;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomPair;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class LSMTreeBenchmark {

    static final Path DIR = Path.of("tree_benchmark");
    static final int NUM_ITEMS = 1000000;
    static final int MEMTABLE_SIZE = 1 << 18;

    static ByteArrayPair[] items;
    static int index = 0;

    LSMTree tree;

    @Setup
    public void setup() throws IOException {
        // setup directory
        if (Files.exists(DIR))
            deleteDir();

        // generate random items
        items = new ByteArrayPair[NUM_ITEMS];
        for (int i = 0; i < NUM_ITEMS; i++)
            items[i] = getRandomPair();

        // setup tree
        tree = new LSMTree(MEMTABLE_SIZE, DIR.toString());
    }

    @TearDown
    public void teardown() throws IOException {
        tree.stop();
        deleteDir();
    }

    private void deleteDir() throws IOException {
        try (var files = Files.list(DIR)) {
            files.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Files.delete(DIR);
    }

    @Benchmark
    public void add() {
        var item = items[index];
        tree.add(item);

        index = (index + 1) % NUM_ITEMS;
    }

    @Benchmark
    public void get(Blackhole bh) {
        var item = items[index];
        var value = tree.get(item.key());

        bh.consume(value);

        index = (index + 1) % NUM_ITEMS;
    }

}
