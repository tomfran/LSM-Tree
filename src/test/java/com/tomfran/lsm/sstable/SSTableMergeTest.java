package com.tomfran.lsm.sstable;

import com.tomfran.lsm.TestUtils;
import com.tomfran.lsm.types.Item;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tomfran.lsm.TestUtils.assertItemEquals;

public class SSTableMergeTest {

    static final String
            MERGE_FILE = "merge.sst",
            TABLE_1_FILE = "test1.sst",
            TABLE_2_FILE = "test2.sst";

    static SSTable merge, first, second;
    static List<Item> firstItems, secondItems, expectedItems;

    @BeforeAll
    public static void setup() {

        firstItems = new ArrayList<>();
        secondItems = new ArrayList<>();
        expectedItems = new ArrayList<>();

        // generate overlapping items
        int n = 10;
        firstItems = generateItems(0, n, false);
        secondItems = generateItems(n - n / 2, n * 2, true);

        // expected is all first items and all second items except the first n/2 overlapping ones
        expectedItems.addAll(firstItems);
        secondItems.stream().skip(n / 2).forEach(expectedItems::add);

        first = new SSTable(TABLE_1_FILE, firstItems, 100, firstItems.size());
        second = new SSTable(TABLE_2_FILE, secondItems, 100, secondItems.size());

        merge = SSTable.merge(MERGE_FILE, 100, first, second);
    }

    @AfterAll
    public static void teardown() {
        merge.close();
        first.close();
        second.close();
        List.of(MERGE_FILE, TABLE_1_FILE, TABLE_2_FILE).forEach(TestUtils::deleteSSTableFiles);
    }

    private static List<Item> generateItems(int start, int end, boolean incr) {
        ArrayList<Item> result = new ArrayList<>();
        for (int i = start; i < end; i++)
            result.add(new Item(new byte[]{(byte) i}, new byte[]{(byte) (i + (incr ? 1 : 0))}));

        return result;
    }

    @Test
    public void shouldGetItems() {
        for (var item : expectedItems) {
            var it = merge.getItem(item.key());
            assert it != null;
            assertItemEquals(item, it);
        }
    }

    @Test
    public void shouldGetItemsInOrder() {
        var it = merge.iterator();
        int i = 0;

        while (it.hasNext()) {
            var item = it.next();
            var expected = expectedItems.get(i++);
            assertItemEquals(expected, item);
        }

        assert i == expectedItems.size() : "expected " + expectedItems.size() + " items, got " + i;
    }

}
