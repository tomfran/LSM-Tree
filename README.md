# LSM tree

An implementation of the Log-Structured Merge Tree (LSM tree) data structure in Java.

**Table of Contents**

1. [Sorted String Table](#SSTable)
2. [Skip-List](#Skip-List)
3. [Benchmarks](#Benchmarks)
4. [Implementation status](#Implementation-status)

---

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
   can stop when we are seeing a key greater than the one we are looking for, or when we reach the end of the table.

The search is as lazy as possible, meaning that we read the minimum amount of data from disk,
for instance, if the next key length is smaller than the one we are looking for, we can skip the whole key-value pair.

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
- `s_1, s_2, ..., s_n`: remaining keys after a sparse index entry, used to exit from search;
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

## Skip-List

A [skip-list](https://en.wikipedia.org/wiki/Skip_list) is a probabilistic data structure that allows fast search,
insertion and deletion of elements in a sorted sequence.

In the LSM tree, it is used as an in-memory data structure to store key-value pairs in sorted order by key.
Once the skip-list reaches a certain size, it is flushed to disk as an SSTable.

### Operations details

The idea of a skip list is similar to a classic linked list. We have nodes with forward pointers, but also levels. We
can think about a
level as a fast lane between nodes. By carefully constructing them at insertion time, searches are faster, as they can
use higher levels to skip unwanted nodes.

Given `n` elements, a skip list has `log(n)` levels, the first level containing all the elements.
By increasing the level, the number of elements is cut roughly by half.

![readme_imgs/skip-list.png](misc/skip-list.png)

To locate an element, we start from the top level and move forward until we find an element greater than the one
we are looking for. Then we move down to the next level and repeat the process until we find the element.

Insertions, deletions, and updates are done by first locating the element, then performing
the operation on the node. All of them have an average time complexity of `O(log(n))`.

---

## Benchmarks

I am using [JMH](https://openjdk.java.net/projects/code-tools/jmh/) to run benchmarks,
the results are obtained on a MacBook Pro (16-inch, 2021) with an M1 Pro processor and 16 GB of RAM.

To run them use `./gradlew jmh`.

### SSTable

- Negative access: the key is not present in the table, hence the Bloom filter will likely stop the search;
- Random access: the key is present in the table, the order of the keys is random.

```

Benchmark Mode Cnt Score Error Units
c.t.l.sstable.SSTableBenchmark.negativeAccess thrpt 10 3541989.316 ± 78933.780 ops/s
c.t.l.sstable.SSTableBenchmark.randomAccess thrpt 10 56157.613 ± 264.314 ops/s

```

### Bloom filter

- Add: add keys to a 1M keys Bloom filter with 0.01 false positive rate;
- Contains: test whether the keys are present in the Bloom filter.

```

Benchmark Mode Cnt Score Error Units
c.t.l.bloom.BloomFilterBenchmark.add thrpt 10 9777191.526 ± 168208.916 ops/s
c.t.l.bloom.BloomFilterBenchmark.contains thrpt 10 10724196.205 ± 20411.741 ops/s

```

### Skip-List

- Get: get keys from a 100k keys skip-list;
- Add/Remove: add and remove keys from a 100k keys skip-list.

```

Benchmark Mode Cnt Score Error Units
c.t.l.memtable.SkipListBenchmark.addRemove thrpt 10 684885.546 ± 21793.787 ops/s
c.t.l.memtable.SkipListBenchmark.get thrpt 10 823423.128 ± 83028.354 ops/s

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
    - [ ] Handle tombstones
- [ ] Skip-List
    - [x] Operations
    - [x] Iterator
- [ ] Tree
    - [x] Operations
    - [x] Background flush
    - [ ] Background compaction
- [ ] Benchmarks
    - [x] SSTable
    - [x] Bloom filter
    - [x] Skip-List
    - [ ] Tree
