package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.tomfran.lsm.TestUtils.getRandomItem;
import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

class SSTableTest {

    static final String TEST_FILE = "/sstable";
    @TempDir
    static Path tempDirectory;
    static SSTable t;
    static List<Item> items;

    @BeforeAll
    public static void setup() {

        var l = new ObjectOpenHashSet<Item>();
        for (int i = 0; i < 10000; i++) {
            l.add(getRandomItem());
        }

        items = l.stream()
                .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                .toList();

        t = new SSTable(tempDirectory + TEST_FILE, items, 100, items.size());
    }

    @AfterAll
    public static void teardown() {
        t.close();
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