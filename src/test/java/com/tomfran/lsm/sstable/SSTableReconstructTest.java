package com.tomfran.lsm.sstable;

import com.tomfran.lsm.types.Item;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.tomfran.lsm.TestUtils.getRandomItem;
import static com.tomfran.lsm.comparator.ByteArrayComparator.compare;
import static com.tomfran.lsm.sstable.SSTable.*;

public class SSTableReconstructTest {

    static final String FILE1 = "/sstable1", FILE2 = "/sstable2";
    @TempDir
    static Path tempDirectory;
    static SSTable t1;

    @BeforeAll
    static void setup() throws IOException {

        var l = new ObjectOpenHashSet<Item>();
        for (int i = 0; i < 10; i++) {
            l.add(getRandomItem());
        }

        var items = l.stream()
                .sorted((a, b) -> compare(a.key(), b.key()))
                .toList();

        t1 = new SSTable(tempDirectory + FILE1, items, 3, items.size());

        for (var end : List.of(INDEX_FILE_EXTENSION, DATA_FILE_EXTENSION, BLOOM_FILE_EXTENSION))
            Files.copy(Path.of(tempDirectory + FILE1 + end), Path.of(tempDirectory + FILE2 + end));

        t1.close();
    }

    @Test
    void shouldReconstruct() {
        var t2 = new SSTable(tempDirectory + FILE2);

        assert t2.size == t1.size;

        assert t2.sparseOffsets.size() == t1.sparseOffsets.size();
        for (var i = 0; i < t2.sparseOffsets.size(); i++)
            assert t2.sparseOffsets.getLong(i) == t1.sparseOffsets.getLong(i);
//
//        assert t2.sparseKeys.size() == t1.sparseKeys.size() : "keys lenght don't match";
//        for (var i = 0; i < t2.sparseKeys.size(); i++)
//            assert compare(t2.sparseKeys.get(i), t1.sparseKeys.get(i)) == 0 : "keys don't match";

        t2.close();
    }


}
