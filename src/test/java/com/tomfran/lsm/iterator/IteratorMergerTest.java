package com.tomfran.lsm.iterator;

import com.tomfran.lsm.skiplist.SkipList;
import com.tomfran.lsm.sstable.SSTable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class IteratorMergerTest {

    SkipList l;
    SSTable t1, t2;
    IteratorMerger it;

    @BeforeEach
    public void setup() {
        l = new SkipList(1);
        t1 = new SSTable(64);
        t2 = new SSTable(64);

        for (int i = 0; i < 5; i++)
            l.put(new byte[]{(byte) i}, new byte[]{(byte) (i)});

        for (int i = 0; i < 10; i++)
            t1.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});

        for (int i = 0; i < 15; i++)
            t2.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 2)});
    }

    @Test
    public void shouldIter() {
        it = new IteratorMerger(new Iterator[]{
                l.iterator(),
                t1.iterator(),
                t2.iterator()
        });

        ObjectArrayList<byte[]> keys = new ObjectArrayList<>();
        ObjectArrayList<byte[]> values = new ObjectArrayList<>();

        while (it.hasNext()) {
            it.next();
            keys.add(it.key());
            values.add(it.value());
        }

        assert keys.size() == 15;
        assert values.size() == 15;
        assert IntStream.range(0, 15)
                .allMatch(i -> keys.get(i)[0] == (values.get(i)[0] - i / 5));
    }

    @Test
    public void shouldIterOtherOrder() {
        it = new IteratorMerger(new Iterator[]{
                t2.iterator(),
                t1.iterator(),
                l.iterator()
        });

        ObjectArrayList<byte[]> keys = new ObjectArrayList<>();
        ObjectArrayList<byte[]> values = new ObjectArrayList<>();

        while (it.hasNext()) {
            it.next();
            keys.add(it.key());
            values.add(it.value());
        }

        assert keys.size() == 15;
        assert values.size() == 15;
        assert IntStream.range(0, 15)
                .allMatch(i -> keys.get(i)[0] == (values.get(i)[0] - 2));
    }

    @Test
    public void shouldDiscardEmptyIterator() {
        var x = t2.iterator();
        while (x.hasNext()) x.next();

        it = new IteratorMerger(new Iterator[]{
                l.iterator(),
                t1.iterator(),
                x
        });

        assert it.q.size() == 2;
    }
}