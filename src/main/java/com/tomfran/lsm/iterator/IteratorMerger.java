package com.tomfran.lsm.iterator;

import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class IteratorMerger implements Iterator {

    protected ObjectHeapPriorityQueue<Pair> q;
    protected Iterator current;
    protected byte[] key;
    protected byte[] value;

    public IteratorMerger(Iterator[] iterators) {
        current = null;
        heapBuilder(iterators);
    }

    private void heapBuilder(Iterator[] iterators) {

        q = new ObjectHeapPriorityQueue<>((a, b) -> {
            int c = compare(a.iter.key(), b.iter.key());
            return c == 0 ? a.rank - b.rank : c;
        });

        for (int i = 0; i < iterators.length; i++) {
            if (!iterators[i].hasNext())
                continue;
            iterators[i].next();
            q.enqueue(new Pair(iterators[i], i));
        }
    }

    @Override
    public void next() {
        Pair head = q.dequeue();
        current = head.iter;
        key = current.key();
        value = current.value();

        if (current.hasNext()) {
            current.next();
            q.enqueue(head);
        }

        removeTies(key);
    }

    private void removeTies(byte[] key) {
        Pair head;
        while (!q.isEmpty() && compare(q.first().iter.key(), key) == 0) {
            head = q.dequeue();
            if (head.iter.hasNext()) {
                head.iter.next();
                q.enqueue(head);
            }
        }
    }

    @Override
    public boolean hasNext() {
        return !q.isEmpty();
    }

    @Override
    public byte[] key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }

    private static final class Pair {
        Iterator iter;
        int rank;

        public Pair(Iterator i, int rank) {
            this.iter = i;
            this.rank = rank;
        }
    }
}
