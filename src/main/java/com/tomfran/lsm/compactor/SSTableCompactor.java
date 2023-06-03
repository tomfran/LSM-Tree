package com.tomfran.lsm.compactor;

import com.tomfran.lsm.iterator.IteratorMerger;
import com.tomfran.lsm.sstable.SSTable;
import com.tomfran.lsm.sstable.SSTableIterator;

import java.util.List;

public class SSTableCompactor {

    List<SSTable> tables;

    public SSTableCompactor(List<SSTable> tables) {
        this.tables = tables;
    }

    public SSTable compact() {
        SSTableIterator[] tableIterators = tables.stream().map(SSTable::iterator).toArray(SSTableIterator[]::new);
        IteratorMerger it = new IteratorMerger(tableIterators);

        SSTable result = new SSTable();
        while (it.hasNext()) {
            it.next();
            result.put(it.key(), it.value());
        }
        return result;
    }


}
