package com.tomfran.lsm.tree;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.Random;

public class LSMTreeReadWriteTest {
    static Random rnd = new java.util.Random();

    public static void main(String[] args) throws InterruptedException {

        LSMTree tree = new LSMTree(
                false, 20, 1000,
                false, 40, 2
        );

        int n = 100_000;
        var written = new Object2ObjectOpenHashMap<byte[], byte[]>();
        try {
            for (int i = 0; i < n; i++) {
                if (rnd.nextInt() % 3 == 0) {
                    written.keySet().stream().findAny().ifPresent(key -> {
                        var value = tree.get(key);
                        if (!Arrays.equals(value, written.get(key))) {
                            System.out.println(Arrays.toString(value));
                            System.out.println(Arrays.toString(written.get(key)));
                            throw new RuntimeException("Value mismatch");
                        }
                    });
                } else {
                    var key = randomBytes();
                    var value = randomBytes();
                    written.put(key, value);
                    tree.put(key, value);
                }
            }
        } finally {
            tree.stop();
        }
        System.out.println(tree);
        System.out.println(written.size());
    }

    public static byte[] randomBytes() {
        byte[] bytes = new byte[rnd.nextInt(80, 100)];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) (rnd.nextInt(0, 256) - 128);

        return bytes;
    }

}
