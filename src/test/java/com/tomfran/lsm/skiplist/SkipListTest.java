package com.tomfran.lsm.skiplist;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

class SkipListTest {

    SkipList t = new SkipList(10);
    RandomGenerator rn = RandomGenerator.of("Xoroshiro128PlusPlus");

    private static byte[] getArr(int i) {
        return new byte[]{(byte) i};
    }

    @Test
    public void shouldAdd() {
        IntOpenHashSet keys = new IntOpenHashSet();
        for (int i = 0; i < 50; i++) {
            var key = rn.nextInt(100);
            keys.add(key);

            var e = getArr(key);
            t.add(e, e);
        }

        assert t.size() == keys.size();

        for (int k : keys) {
            var key = getArr(k);
            var value = t.get(key);

            assert value != null;
            assert ByteArrayComparator.compare(value, key) == 0;
        }

    }


}