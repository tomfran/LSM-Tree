package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.tomfran.lsm.TestUtils.getRandomItem;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SSTableBenchmark {

    static final Path DIR = Path.of("sst_benchmark");
    static final int NUM_ITEMS = 100000;
    static final int SAMPLE_SIZE = NUM_ITEMS / 1000;

    static Item[] insertedArray;
    static Item[] skippedArray;
    static SSTable sstable;

    static int index = 0;

    @Setup
    public void setup() throws IOException {
        // setup directory
        if (Files.exists(DIR))
            deleteDir();

        Files.createDirectory(DIR);

        // generate random items
        var l = new ObjectOpenHashSet<Item>();
        for (int i = 0; i < NUM_ITEMS * 2; i++) {
            l.add(getRandomItem());
        }

        // sort and divide into inserted and skipped
        var items = l.stream()
                .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                .toList();

        var inserted = new ObjectArrayList<Item>();
        var skipped = new ObjectArrayList<Item>();

        for (int i = 0; i < items.size(); i++) {
            var e = items.get(i);
            if (i % 2 == 0)
                inserted.add(e);
            else
                skipped.add(e);
        }

        sstable = new SSTable(DIR + "/sst", inserted, SAMPLE_SIZE, inserted.size());

        // shuffle to avoid sequential access
        Collections.shuffle(inserted);
        Collections.shuffle(skipped);
        insertedArray = inserted.toArray(Item[]::new);
        skippedArray = skipped.toArray(Item[]::new);
    }

    @TearDown
    public void teardown() throws IOException {
        sstable.close();
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
    public void randomAccess(Blackhole bh) {
        var item = insertedArray[index];
        var it = sstable.getItem(item.key());

        bh.consume(it);

        index = (index + 1) % insertedArray.length;
    }

    @Benchmark
    public void negativeAccess(Blackhole bh) {
        var item = skippedArray[index];
        var it = sstable.getItem(item.key());

        bh.consume(it);

        index = (index + 1) % skippedArray.length;
    }
}
