package com.tomfran.lsm.tree;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.stream.IntStream;

import static com.tomfran.lsm.TestUtils.getRandomPair;
import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

class LSMTreeTest {

    @TempDir
    static Path tempDirectory;
    final int maxSize = 10, levelSize = 15;

    @Test
    public void writeFlush() throws InterruptedException {
        LSMTree tree = new LSMTree(maxSize, levelSize, tempDirectory + "/test1");

        IntStream.range(0, 10).forEach(i -> tree.add(getRandomPair(2, 2)));

        Thread.sleep(500);

        assert tree.mutableMemtable.byteSize() >= 1 : "mutable memtable size is " + tree.mutableMemtable.byteSize();
        assert !tree.levels.get(0).isEmpty() : "table is null";

        tree.stop();
    }

    @Test
    public void writeFlow() throws InterruptedException {
        LSMTree tree = new LSMTree(maxSize, levelSize, tempDirectory + "/test2");

        Object2ObjectArrayMap<byte[], byte[]> items = new Object2ObjectArrayMap<>();

        IntStream.range(0, 10).forEach(i -> {
            var it = getRandomPair(2, 2);
            tree.add(it);
            items.put(it.key(), it.value());
        });


        Thread.sleep(1000);

        for (var it : items.entrySet())
            assert compare(tree.get(it.getKey()), it.getValue()) == 0;

        tree.stop();
    }

}