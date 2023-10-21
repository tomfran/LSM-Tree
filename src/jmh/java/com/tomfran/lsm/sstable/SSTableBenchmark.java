package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomPair;
import static com.tomfran.lsm.utils.BenchmarkUtils.deleteDir;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SSTableBenchmark {


    @Benchmark
    public void randomAccess(TableState s, Blackhole bh) {
        var item = s.insertedArray[s.index];
        var it = s.sstable.get(item.key());

        bh.consume(it);

        s.index = (s.index + 1) % s.insertedArray.length;
    }

    @Benchmark
    public void negativeAccess(TableState s, Blackhole bh) {
        var item = s.skippedArray[s.index];
        var it = s.sstable.get(item.key());

        bh.consume(it);

        s.index = (s.index + 1) % s.skippedArray.length;
    }

    @State(Scope.Thread)
    public static class TableState {

        final int NUM_ITEMS = 100000;
        final int SAMPLE_SIZE = 1000;

        ByteArrayPair[] insertedArray;
        ByteArrayPair[] skippedArray;
        SSTable sstable;
        Path dir = Path.of("sst_benchmark_");

        int index = 0;

        @Setup
        public void setup() throws IOException {
            // setup directory
            dir = Path.of(dir.toString() + System.currentTimeMillis());

            if (Files.exists(dir))
                deleteDir(dir);

            Files.createDirectory(dir);

            var l = new ObjectOpenHashSet<ByteArrayPair>();
            for (int i = 0; i < NUM_ITEMS * 2; i++) {
                l.add(getRandomPair());
            }

            var items = l.stream()
                         .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                         .toList();

            var inserted = new ObjectArrayList<ByteArrayPair>();
            var skipped = new ObjectArrayList<ByteArrayPair>();

            for (int i = 0; i < items.size(); i++) {
                var e = items.get(i);
                if (i % 2 == 0)
                    inserted.add(e);
                else
                    skipped.add(e);
            }

            sstable = new SSTable(dir + "/sst", inserted.iterator(), SAMPLE_SIZE);

            Collections.shuffle(inserted);
            Collections.shuffle(skipped);
            insertedArray = inserted.toArray(ByteArrayPair[]::new);
            skippedArray = skipped.toArray(ByteArrayPair[]::new);
        }

        @TearDown
        public void teardown() {
            sstable.close();
            deleteDir(dir);
        }

    }

}
