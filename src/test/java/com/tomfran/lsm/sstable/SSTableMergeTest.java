package com.tomfran.lsm.sstable;

import com.tomfran.lsm.comparator.ByteArrayComparator;
import com.tomfran.lsm.memtable.Memtable;
import com.tomfran.lsm.types.ByteArrayPair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.tomfran.lsm.TestUtils.assertPairEqual;

public class SSTableMergeTest {

    static final String MERGE_FILE = "/merge", TABLE_FILE = "/test";
    @TempDir
    static Path tempDirectory;
    static Memtable first;
    static SSTable merge, second;
    static List<ByteArrayPair> firstItems, secondItems, expectedItems;

    @BeforeAll
    public static void setup() {

        firstItems = new ArrayList<>();
        secondItems = new ArrayList<>();
        expectedItems = new ArrayList<>();

        // generate overlapping items
        int n = 10;

        firstItems = generatePairList(0, n, false);
        secondItems = generatePairList(n - n / 2, n * 2, true);

        // expected is all first items and all second items except the first n/2 overlapping ones
        expectedItems.addAll(firstItems);
        secondItems.stream().skip(n / 2).forEach(expectedItems::add);


        first = new Memtable();
        firstItems.forEach(first::add);
        second = new SSTable(tempDirectory + TABLE_FILE, secondItems, 100);

        merge = SSTable.merge(tempDirectory + MERGE_FILE, 100, first, second);
    }

    @AfterAll
    public static void teardown() {
        merge.close();
        second.close();
    }

    private static List<ByteArrayPair> generatePairList(int start, int end, boolean incr) {
        ArrayList<ByteArrayPair> result = new ArrayList<>();
        for (int i = start; i < end; i++)
            result.add(new ByteArrayPair(new byte[]{(byte) i}, new byte[]{(byte) (i + (incr ? 1 : 0))}));

        return result;
    }

    @Test
    public void shouldGetItems() {
        for (var item : expectedItems) {
            var val = merge.get(item.key());
            assert val != null;
            assert ByteArrayComparator.compare(item.value(), val) == 0;
        }
    }

    @Test
    public void shouldGetItemsInOrder() {
        var it = merge.iterator();
        int i = 0;

        while (it.hasNext()) {
            var item = it.next();
            var expected = expectedItems.get(i++);
            assertPairEqual(expected, item);
        }

        assert i == expectedItems.size() : "expected " + expectedItems.size() + " items, got " + i;
    }

}
