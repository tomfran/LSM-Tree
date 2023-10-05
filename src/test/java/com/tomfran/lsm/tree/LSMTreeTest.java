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

    @Test
    public void writeFlush() throws InterruptedException {
        int maxSize = 10;

        LSMTree tree = new LSMTree(maxSize, tempDirectory + "/test1");

        IntStream.range(0, maxSize + 2).forEach(i -> tree.add(getRandomPair()));

        Thread.sleep(2000);

        assert tree.mutableMemtable.size() == 1 : "mutable memtable size is " + tree.mutableMemtable.size();
        assert tree.table != null : "table is null";
    }

    @Test
    public void writeFlow() throws InterruptedException {
        int maxSize = 10;

        LSMTree tree = new LSMTree(maxSize, tempDirectory + "/test2");

        Object2ObjectArrayMap<byte[], byte[]> items = new Object2ObjectArrayMap<>();

        IntStream.range(0, 5 * maxSize).forEach(i -> {
            var it = getRandomPair();
            tree.add(it);
            items.put(it.key(), it.value());
        });

        Thread.sleep(5000);

        for (var it : items.entrySet())
            assert compare(tree.get(it.getKey()), it.getValue()) == 0;
    }

}