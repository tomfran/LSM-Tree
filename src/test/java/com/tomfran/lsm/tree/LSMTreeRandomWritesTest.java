package com.tomfran.lsm.tree;

import java.util.concurrent.Executors;

import static com.tomfran.lsm.sstable.BloomFilterCollisionBenchmark.randomBytes;

public class LSMTreeRandomWritesTest {

    public static void main(String[] args) {

        LSMTree tree = new LSMTree(true, 500, 100);

        var executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 1000000; i++) {
            executor.submit(() -> {
                var key = randomBytes();
                var value = randomBytes();
                tree.put(key, value);
            });
        }
    }

}
