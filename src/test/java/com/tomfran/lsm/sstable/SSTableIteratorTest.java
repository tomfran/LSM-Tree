package com.tomfran.lsm.sstable;

import org.junit.jupiter.api.Test;

class SSTableIteratorTest {

    SSTable t;
    SSTableIterator it;

    @Test
    public void shouldGetAll() {
        int limit = 126;
        t = new SSTable(64);
        for (int i = 0; i < limit; i++) {
            t.add(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});
        }

        it = t.iterator();
        assert it.hasNext();

        for (int i = 0; i < limit; i++) {
            it.next();
            assert it.key()[0] == i;
            assert it.value()[0] == i + 1;
        }
        assert !it.hasNext();
    }

}