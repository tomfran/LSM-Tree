package com.tomfran.lsm.utils;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Merges multiple sorted Iterators into a single sorted Iterator.
 *
 * @param <T> The type of the elements in the Iterators.
 */
public class IteratorMerger<T> implements Iterator<T> {

    Iterator<T>[] iterators;
    ObjectHeapPriorityQueue<Pair<T, Integer>> queue;

    @SafeVarargs
    public IteratorMerger(Comparator<T> cmp, Iterator<T>... iterators) {
        this.iterators = iterators;
        queue = new ObjectHeapPriorityQueue<>((a, b) -> {
            int cmpResult = cmp.compare(a.first(), b.first());
            return cmpResult == 0 ? Integer.compare(a.second(), b.second()) : cmpResult;
        });

        for (int i = 0; i < iterators.length; i++) {
            if (iterators[i].hasNext())
                queue.enqueue(new ObjectIntMutablePair<>(iterators[i].next(), i));
        }
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public T next() {
        Pair<T, Integer> top = queue.dequeue();

        T result = top.first();

        int index = top.second();
        if (index == -1)
            return result;

        T next = iterators[index].next();
        int newIndex = iterators[index].hasNext() ? index : -1;
        queue.enqueue(top.first(next).second(newIndex));

        return result;
    }
}
