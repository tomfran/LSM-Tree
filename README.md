# LSM tree

A simple implementation of the Log-Structured Merge Tree (LSM tree) data structure in Java.

### SSTable

Sorted String Table (SSTable) is a file containing key-value pairs in sorted order by key.
It is immutable and append-only.

**Components of SSTable:**

- Data: key-value pairs in sorted order by key, stored in a file;
- Index: sparse index containing key and offset of the corresponding key-value pair in the data;
- Bloom filter: a probabilistic data structure used to test whether a key is in the SSTable.

**Key lookup:**

- Use the Bloom filter to test whether the key might be in the SSTable;
- If the key might be present, use binary search on the index to find a close position in the data;
- Scan the data from the position found in the previous step to find the key-value pair.

## TO-DO

- [ ] SSTable
    - [x] Init
    - [x] Read
    - [x] Compaction
    - [ ] Bloom filter
    - [ ] Compression
- [ ] Memtable
- [ ] LSM tree
