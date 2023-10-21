package com.tomfran.lsm.utils;

import com.tomfran.lsm.tree.LSMTree;
import com.tomfran.lsm.types.ByteArrayPair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static com.tomfran.lsm.TestUtils.getRandomPair;

public class BenchmarkUtils {

    public static LSMTree initTree(Path dir, int memSize, int levelSize) throws IOException {
        // setup directory
        if (Files.exists(dir))
            deleteDir(dir);

        // setup tree
        return new LSMTree(memSize, levelSize, dir.toString());
    }

    public static ByteArrayPair[] fillItems(int n) {
        ByteArrayPair[] items = new ByteArrayPair[n];
        for (int i = 0; i < n; i++)
            items[i] = getRandomPair();
        return items;
    }

    public static void shuffleItems(ByteArrayPair[] v) {
        var rn = new Random();
        for (int i = 0; i < v.length; i++) {
            var tmp = v[i];
            int j = rn.nextInt(i, v.length);
            v[i] = v[j];
            v[j] = tmp;
        }
    }

    public static void deleteDir(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            files.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Files.delete(dir);
    }

}
