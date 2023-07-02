package com.tomfran.lsm.tree;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class LSMTreeBenchmark {

    final static ObjectArrayList<byte[]> keys = new ObjectArrayList<>();
    final static ObjectArrayList<byte[]> values = new ObjectArrayList<>();
    static RandomGenerator r = RandomGenerator.getDefault();
    static int N = 1_000_000;
    static int KEY_SIZE = 8;
    static int VALUE_SIZE = 8;
    static int FLUSH_STEP = N / 10;
    static int[] keyOrder;

    public static void main(String[] args) {

        generateRandomData();
        LSMTree tree = new LSMTree(false, 0, 0, false, 0, 0);

        long start, end;

        start = System.currentTimeMillis();

        for (int i = 0; i < N; i++) {
            tree.put(keys.get(i), values.get(i));

            if (i % FLUSH_STEP == 0 && i != 0) {
                tree.sync();
                tree.flushLastImmutableMemtable();
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Time taken to insert " + N + " entries: " + (end - start) + "ms");

        start = System.currentTimeMillis();

        for (int i = 0; i < N; i++) {
            if (i % (N / 10) == 0 && i != 0) {
                System.out.println("Progress: " + i + "/" + N);
            }
            var key = keys.get(keyOrder[i]);
            var value = tree.get(key);

            assert value != null;
        }

        end = System.currentTimeMillis();
        System.out.println("Time taken to get " + N + " entries: " + (end - start) + "ms");
    }

    private static void generateRandomData() {
        keyOrder = IntStream.range(0, N).toArray();

        for (int i = 0; i < N; i++) {
            keys.add(randomByteArray(KEY_SIZE));
            values.add(randomByteArray(VALUE_SIZE));
            int tmp = keyOrder[r.nextInt(i, N)];
            keyOrder[i] = keyOrder[tmp];
            keyOrder[tmp] = i;
        }
    }

    private static byte[] randomByteArray(int size) {
        byte[] bytes = new byte[size];

        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (r.nextInt(256) - 128);
        }

        return bytes;
    }
}
