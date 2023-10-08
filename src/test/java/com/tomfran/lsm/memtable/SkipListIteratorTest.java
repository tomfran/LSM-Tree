package com.tomfran.lsm.memtable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tomfran.lsm.TestUtils.getRandomPair;
import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

public class SkipListIteratorTest {

    static int NUM_ITEMS = 10;
    static SkipList list;
    static List<ByteArrayPair> items;

    @BeforeAll
    static void setup() {

        list = new SkipList(NUM_ITEMS);

        // generate random items
        var l = new ObjectOpenHashSet<ByteArrayPair>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            l.add(getRandomPair());
        }

        items = l.stream()
                 .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                 .toList();

        items.forEach(list::add);
    }

    @Test
    public void iteratorTest() {
        var it = list.iterator();
        var it2 = items.iterator();

        while (it.hasNext()) {
            var a = it.next();
            var b = it2.next();
            assert compare(a.key(), b.key()) == 0;
            assert compare(a.value(), b.value()) == 0;
        }

        assert !it2.hasNext();
    }

}
