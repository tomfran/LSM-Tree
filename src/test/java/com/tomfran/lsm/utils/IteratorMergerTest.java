package com.tomfran.lsm.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class IteratorMergerTest {

    @Test
    public void shouldMerge() {

        List<Iterator<Integer>> iteratorList = IntStream.range(0, 10)
                                                        .mapToObj(i -> IntStream.range(i * 10, i * 10 + 10).boxed().iterator())
                                                        .collect(Collectors.toList());

        Collections.shuffle(iteratorList);

        IteratorMerger<Integer> merger = new IteratorMerger<>(
                iteratorList.<Iterator<Integer>>toArray(Iterator[]::new)
        );

        int expected = 0;
        while (merger.hasNext()) {
            assert merger.next() == expected : "Expected " + expected + " but got " + (expected - 1);
            expected++;
        }

        assert merger.next() == null;
    }

}