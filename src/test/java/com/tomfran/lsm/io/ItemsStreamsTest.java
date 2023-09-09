package com.tomfran.lsm.io;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.stream.IntStream;

import static com.tomfran.lsm.TestUtils.assertItemEquals;
import static com.tomfran.lsm.TestUtils.getRandomItem;

class ItemsStreamsTest {

    static final String FILENAME = "/items.data";
    @TempDir
    static Path tempDirectory;
    static ItemsInputStream in;
    static ItemsOutputStream out;

    static ObjectArrayList<Item> items;

    @BeforeAll
    public static void setup() {
        out = new ItemsOutputStream(tempDirectory + "/items.data");

        items = new ObjectArrayList<>();

        IntStream.range(0, 1000)
                .mapToObj(i -> getRandomItem())
                .forEach(e -> {
                    items.add(e);
                    out.writeItem(e);
                });

        out.close();

        in = new ItemsInputStream(tempDirectory + FILENAME);
    }

    @AfterAll
    public static void teardown() {
        in.close();
        out.close();
    }

    @Test
    public void shouldReadItem() {
        for (var item : items) {
            var it = in.readItem();
            assert it != null;
            assertItemEquals(item, it);
        }
    }

}