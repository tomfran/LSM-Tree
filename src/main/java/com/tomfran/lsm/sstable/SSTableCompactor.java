package com.tomfran.lsm.sstable;

import com.tomfran.lsm.iterator.IteratorMerger;

import java.util.List;

public class SSTableCompactor {

    List<SSTable> tables;

    public SSTableCompactor(List<SSTable> tables) {
        this.tables = tables;
    }

    public SSTable compact() {
        SSTableIterator[] tableIterators = tables.stream().map(SSTable::iterator).toArray(SSTableIterator[]::new);
        IteratorMerger it = new IteratorMerger(tableIterators);

        int size = 0;
        for (SSTable table : tables)
            size += table.size();

        SSTable result = new SSTable(size);
        while (it.hasNext()) {
            it.next();
            result.put(it.key(), it.value());
        }
        return result;
    }


}
