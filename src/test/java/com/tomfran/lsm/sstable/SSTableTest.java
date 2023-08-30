package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Random;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

class SSTableTest {

    static final String TEST_FILE = "test.sst";

    static SSTable t;
    static List<Item> items;

    @BeforeAll
    public static void setup() {

        Random rn = new Random();
        var l = new ObjectOpenHashSet<Item>();

        for (int i = 0; i < 10000; i++) {
            byte[] key = new byte[rn.nextInt(10, 500)];
            byte[] value = new byte[rn.nextInt(10, 500)];
            rn.nextBytes(key);
            rn.nextBytes(value);
            l.add(new Item(key, value));
        }

        items = l.stream()
                .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                .toList();

        t = new SSTable(TEST_FILE, items, 100, items.size());
    }

    @AfterAll
    public static void teardown() {
        t.close();
        new File(TEST_FILE).delete();
    }

    @Test
    public void shouldFindItems() {
        for (var item : items) {
            var it = t.getItem(item.key());
            assert it != null;
            assert compare(item.key(), it.key()) == 0;
            assert compare(item.value(), it.value()) == 0;
        }
    }

    @Test
    public void iteratorTest() {
        var it = t.iterator();
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