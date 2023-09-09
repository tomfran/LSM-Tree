# LSM tree

An implementation of the Log-Structured Merge Tree (LSM tree) data structure in Java.

## SSTable

Sorted String Table (SSTable) is a collection of files modelling key-value pairs in sorted order by key.
It is used as a persistent storage for the LSM tree.

### Components

- _Data_: key-value pairs in sorted order by key, stored in a file;
- _Sparse index_: sparse index containing key and offset of the corresponding key-value pair in the data;
- _Bloom filter_: a [probabilistic data structure](https://en.wikipedia.org/wiki/Bloom_filter) used to test whether a
  key is in the SSTable.

### Key lookup

The basic idea is to use the sparse index to find the key-value pair in the data file.
The steps are:

1. Use the Bloom filter to test whether the key might be in the table;
2. If the key might be present, use binary search on the index to find the maximum lower bound of the key;
3. Scan the data from the position found in the previous step to find the key-value pair. The search
   can stop when the we are seeing a key greater than the one we are looking for.

### Persistence

A table is persisted to disk when it is created. A base filename is defined, and three files are present:

- `<base_filename>.data`: data file;
- `<base_filename>.index`: index file;
- `<base_filename>.bloom`: bloom filter file.

**Data format**

- `n`: number of key-value pairs;
- `<key_len_1, value_len_1, key_1, value_1, ... key_n, value_n>`: key-value pairs.

**Index format**

- `s`: number of entries in the whole table;
- `n`: number of entries in the index;
- `o_1, o_2 - o_1, ..., o_n - o_n-1`: offsets of the key-value pairs in the data file, skipping
  the first one;
- `<key_len_1, key_1, ... key_len_n, key_n>`: keys in the index.

**Filter format**

- `m`: number of bits in the bloom filter;
- `k`: number of hash functions;
- `n`: size of underlying long array;
- `b_1, b_2, ..., b_n`: bits of the bloom filter.

To save space, all integers are stored
in [variable-length encoding](https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html),
and offsets in the index are stored as [deltas](https://en.wikipedia.org/wiki/Delta_encoding).

---

## Benchmarks

I am using [JMH](https://openjdk.java.net/projects/code-tools/jmh/) to run benchmarks.

### SSTable

- Negative access: the key is not present in the table, hence the Bloom filter will likely stop the search;
- Random access: the key is present in the table, the order of the keys is random.

```
Benchmark                         Mode  Cnt        Score       Error  Units
SSTableBenchmark.negativeAccess  thrpt   10  2449836.577 ± 44131.648  ops/s
SSTableBenchmark.randomAccess    thrpt   10    33154.515 ±  1522.062  ops/s
```

---

## Implementation status

- [x] SSTable
    - [x] Init
    - [x] Read
    - [x] Compaction
    - [x] Ints compression
    - [x] Bloom filter
    - [x] Indexes persistence
    - [x] File initialization
- [ ] Memtable
- [ ] Tree
