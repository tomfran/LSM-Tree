package com.tomfran.lsm.utils;

import com.tomfran.lsm.tree.LSMTree;
import com.tomfran.lsm.types.ByteArrayPair;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static com.tomfran.lsm.TestUtils.getRandomPair;

public class BenchmarkUtils {

    public static LSMTree initTree(Path dir, int memSize, int immutableSize) {
        if (Files.exists(dir))
            deleteDir(dir);

        return new LSMTree(memSize, immutableSize, dir.toString());
    }

    public static void stopTreeAndCleanDisk(LSMTree tree, Path dir) {
        try {
            tree.stop();
            Thread.sleep(5000);
        } catch (Exception ignored) {
        }

        deleteDir(dir);
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

    public static void deleteDir(Path dir) {
        try {
            try (var f = Files.walk(dir)) {
                f.map(Path::toFile).forEach(File::delete);
            }
            Files.delete(dir);
        } catch (Exception ignored) {
        }
    }

}
