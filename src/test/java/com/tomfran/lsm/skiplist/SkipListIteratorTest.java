package com.tomfran.lsm.skiplist;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

class SkipListIteratorTest {

    static final int NUM_ELEMENTS = 100;

    SkipList l = new SkipList(5);
    SkipListIterator it = l.iterator();

    @Test
    public void shouldNotIterate() {
        assert !it.hasNext();
        assert it.key() == null;
        assert it.value() == null;
    }

    @Test
    public void shouldIterate() {

        RandomGenerator rn = RandomGenerator.getDefault();
        int[] arr = IntStream.range(0, NUM_ELEMENTS).toArray();
        for (int i = 0; i < arr.length; i++) {
            int j = rn.nextInt(i, arr.length);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
            l.add(new byte[]{(byte) arr[i]}, new byte[]{(byte) (arr[i] + 1)});
        }

        assert it.hasNext();
        assert it.key() == null;
        assert it.value() == null;

        int i = 0;
        while (it.hasNext()) {
            it.next();
            assert it.key()[0] == i;
            assert it.value()[0] == i + 1;
            i++;
        }

        assert i == NUM_ELEMENTS;
        assert !it.hasNext();
    }
}