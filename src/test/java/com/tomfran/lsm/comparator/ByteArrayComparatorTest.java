package com.tomfran.lsm.comparator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class ByteArrayComparatorTest {

    static Stream<Arguments> shouldCompare() {
        return Stream.of(
                Arguments.of(new byte[]{1}, new byte[]{1, 2}, -1),
                Arguments.of(new byte[]{1, 2}, new byte[]{1}, 1),
                Arguments.of(new byte[]{1, 2}, new byte[]{1, 3}, -1),
                Arguments.of(new byte[]{1, 2}, new byte[]{1, 1}, 1),
                Arguments.of(new byte[]{1, 2}, new byte[]{1, 2}, 0)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void shouldCompare(byte[] a, byte[] b, int expected) {
        assert ByteArrayComparator.compare(a, b) == expected;
    }

}