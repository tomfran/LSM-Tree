package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.tomfran.lsm.TestUtils.getRandomPair;
import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

class SSTableTest {

    static final String TEST_FILE = "/sstable";
    static final int NUM_ITEMS = 10;
    static final int SAMPLE_SIZE = NUM_ITEMS / 3;

    @TempDir
    static Path tempDirectory;

    static SSTable t;
    static ObjectArrayList<ByteArrayPair> inserted;
    static ObjectArrayList<ByteArrayPair> skipped;

    @BeforeAll
    public static void setup() {
        // generate random items
        var l = new ObjectOpenHashSet<ByteArrayPair>();
        for (int i = 0; i < NUM_ITEMS * 2; i++) {
            l.add(getRandomPair());
        }

        // sort and divide into inserted and skipped
        var items = l.stream()
                .sorted((a, b) -> ByteArrayComparator.compare(a.key(), b.key()))
                .toList();

        inserted = new ObjectArrayList<>();
        skipped = new ObjectArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            var e = items.get(i);
            if (i % 2 == 0)
                inserted.add(e);
            else
                skipped.add(e);
        }

        t = new SSTable(tempDirectory + TEST_FILE, inserted, SAMPLE_SIZE, inserted.size());
    }

    @AfterAll
    public static void teardown() {
        t.close();
    }

    @Test
    public void shouldFindItems() {
        for (var item : inserted) {
            var val = t.get(item.key());
            assert val != null;
            assert compare(item.value(), val) == 0;
        }
    }

    @Test
    public void shouldNotFind() {
        for (var item : skipped)
            assert t.get(item.key()) == null;
    }

    @Test
    public void iteratorTest() {
        var it = t.iterator();
        var it2 = inserted.iterator();

        while (it.hasNext()) {
            var a = it.next();
            var b = it2.next();
            assert compare(a.key(), b.key()) == 0;
            assert compare(a.value(), b.value()) == 0;
        }

        assert !it2.hasNext();
    }
}