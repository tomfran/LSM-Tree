package com.tomfran.lsm.memtable;

import com.tomfran.lsm.TestUtils;
import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.stream.Stream;

import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;

@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class SkipListTest {

    static SkipList l;
    static ObjectArrayList<ByteArrayPair> items;

    @BeforeAll
    public static void setup() {
        l = new SkipList(100);
        items = new ObjectArrayList<>();

        Stream.generate(TestUtils::getRandomPair)
                .limit(100)
                .forEach(items::add);

        items.forEach(l::add);
    }

    @Test
    @Order(1)
    public void shouldFind() {
        for (ByteArrayPair item : items) {
            var found = l.get(item.key());
            assert found != null;
            assert compare(found.key(), item.key()) == 0;
            assert compare(found.value(), item.value()) == 0;
        }
    }

    @Test
    @Order(2)
    public void shouldRemove() {
        for (ByteArrayPair item : items.subList(0, 50)) {
            l.remove(item.key());
        }

        for (int i = 0; i < 100; i++) {
            var item = items.get(i);
            var found = l.get(item.key());

            if (i < 50) {
                assert found == null;
            } else {
                assert found != null;
                assert compare(found.key(), item.key()) == 0;
                assert compare(found.value(), item.value()) == 0;
            }
        }
    }

}