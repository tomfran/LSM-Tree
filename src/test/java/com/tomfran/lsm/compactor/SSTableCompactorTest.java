package com.tomfran.lsm.compactor;

import com.tomfran.lsm.sstable.SSTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

class SSTableCompactorTest {

    static List<SSTable> tables;
    SSTableCompactor compactor;

    @BeforeAll
    public static void setup() {
        SSTable t1 = new SSTable(128);
        for (int i = 0; i < 100; i++)
            t1.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});

        SSTable t2 = new SSTable(128);
        for (int i = 0; i < 120; i++)
            t2.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 2)});

        tables = List.of(t1, t2);
    }

    @Test
    public void f() {
        compactor = new SSTableCompactor(tables);

        SSTable result = compactor.compact();

        for (int i = 0; i < 120; i++) {
            var key = new byte[]{(byte) i};
            var value = result.get(key);

            assert value != null;
            assert value[0] == (byte) (i + (i / 100) + 1);
        }
    }

}