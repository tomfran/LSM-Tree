package com.tomfran.lsm.utils;

import java.util.Iterator;

/**
 * Skip duplicates in a sorted iterator by keeping only the first one.
 * <p>
 * Reads after the last element of the last Iterator will return null.
 *
 * @param <T> The type of the elements in the Iterators.
 */
public class UniqueSortedIterator<T extends Comparable<T>> implements Iterator<T> {

    Iterator<T> iterator;
    private T last;

    public UniqueSortedIterator(Iterator<T> iterator) {
        this.iterator = iterator;
        last = iterator.next();
    }

    @Override
    public boolean hasNext() {
        return last != null;
    }

    @Override
    public T next() {
        T next = iterator.next();
        while (next != null && last.compareTo(next) == 0)
            next = iterator.next();

        T toReturn = last;
        last = next;

        return toReturn;
    }

}
