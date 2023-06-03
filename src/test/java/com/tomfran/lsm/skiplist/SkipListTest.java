package com.tomfran.lsm.skiplist;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

class SkipListTest {

    SkipList t;
    RandomGenerator rn = RandomGenerator.getDefault();

    private static byte[] getArr(int i) {
        return new byte[]{(byte) i};
    }

    @BeforeEach
    public void setup() {
        t = new SkipList(128);
    }

    @Test
    public void shouldAdd() {
        IntOpenHashSet keys = new IntOpenHashSet();
        for (int i = 0; i < 50; i++) {
            var key = rn.nextInt(100);
            keys.add(key);

            var e = getArr(key);
            t.put(e, e);
        }

        assert t.size() == keys.size();

        for (int k : keys) {
            var key = getArr(k);
            var value = t.get(key);

            assert value != null;
            assert ByteArrayComparator.compare(value, key) == 0;
        }
    }

    @Test
    public void shouldDelete() {
        t.put(getArr(1), getArr(2));
        t.put(getArr(2), getArr(3));
        assert t.get(getArr(1)) != null;
        assert t.get(getArr(2)) != null;

        t.delete(getArr(1));
        assert t.get(getArr(1)) == null;
    }

}