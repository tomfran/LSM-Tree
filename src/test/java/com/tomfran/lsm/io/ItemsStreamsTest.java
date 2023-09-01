package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

class ItemsStreamsTest {

    static final String FILENAME = "io-test.sst";

    static ItemsInputStream in;
    static ItemsOutputStream out;

    @BeforeAll
    public static void setup() {
        out = new ItemsOutputStream(FILENAME);

        Item item = new Item(new byte[]{1, 2, 3}, new byte[]{4, 5, 6});
        out.writeItem(item);

        out.close();

        in = new ItemsInputStream(FILENAME);
    }

    @AfterAll
    public static void teardown() {
        in.close();
        out.close();

        new File(FILENAME).delete();
    }

    @Test
    public void shouldReadItem() {
        Item item = in.readItem();
        assert item != null;
        assert compare(item.key(), new byte[]{1, 2, 3}) == 0;
        assert compare(item.value(), new byte[]{4, 5, 6}) == 0;
    }

}