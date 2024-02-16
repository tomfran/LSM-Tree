package com.tomfran.lsm.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;

class UniqueSortedIteratorTest {

    @Test
    public void test() {
        var unique = new UniqueSortedIterator<>(new DummyIterator());

        ArrayList<Integer> res = new ArrayList<>();

        while (unique.hasNext())
            res.add(unique.next());

        assert res.size() == 10;
        for (int i = 0; i < 10; i++)
            assert res.get(i) == i;
    }

    private static class DummyIterator implements Iterator<Integer> {

        int repeat = 3;
        int size = 0;

        @Override
        public boolean hasNext() {
            return size < 10;
        }

        @Override
        public Integer next() {
            if (size >= 10)
                return null;

            if (repeat == 0) {
                repeat = 3;
                return size++;
            }

            repeat--;
            return size;
        }

    }

}