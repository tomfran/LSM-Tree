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

    @State(Scope.Thread)
    public static class BaseState {

        final Path DIR = Path.of("tree_benchmark");
        final int NUM_ITEMS = 1000000;
        final int MEMTABLE_SIZE = 1 << 18;
        final int LEVEL_SIZE = 5;

        ByteArrayPair[] items;
        int index = 0;

        LSMTree tree;

        @Setup
        public void setup() throws IOException {
            treeSetup();
        }

        @TearDown
        public void teardown() throws IOException {
            tree.stop();
            deleteDir();
        }

        public void treeSetup() throws IOException {
            // setup directory
            if (Files.exists(DIR))
                deleteDir();

            // generate random items
            items = new ByteArrayPair[NUM_ITEMS];
            for (int i = 0; i < NUM_ITEMS; i++)
                items[i] = getRandomPair();

            // setup tree
            tree = new LSMTree(MEMTABLE_SIZE, LEVEL_SIZE, DIR.toString());
        }

        public void deleteDir() throws IOException {
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

    }

    @State(Scope.Thread)
    public static class GetState extends BaseState {

        @Setup
        public void setup() throws IOException {
            treeSetup();
            for (var it : items)
                tree.add(it);
        }

    }

    @Benchmark
    public void add(BaseState s, Blackhole bh) {
        var item = s.items[s.index];
        s.tree.add(item);

        s.index = (s.index + 1) % s.NUM_ITEMS;
    }

    @Benchmark
    public void get(GetState s, Blackhole bh) {
        var item = s.items[s.index];
        var value = s.tree.get(item.key());

        bh.consume(value);

        s.index = (s.index + 1) % s.NUM_ITEMS;
    }

}
