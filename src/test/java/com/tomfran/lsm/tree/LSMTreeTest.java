package com.tomfran.lsm.tree;

import org.junit.jupiter.api.Test;

class LSMTreeTest {

    LSMTree tree;

    @Test
    public void f() {
        tree = new LSMTree();

        for (int i = 20; i < 100; i++)
            tree.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});

        tree.flushLastMemtable();
        tree.addMemtable();

        for (int i = 10; i < 20; i++)
            tree.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});

        tree.addMemtable();

        for (int i = 0; i < 10; i++)
            tree.put(new byte[]{(byte) i}, new byte[]{(byte) (i + 1)});

        for (int i = 0; i < 100; i++) {
            var key = new byte[]{(byte) i};
            var value = tree.get(key);

            assert value != null;
            assert value[0] == (byte) (i + 1);
        }

        assert tree.get(new byte[]{(byte) 100}) == null;
    }


}