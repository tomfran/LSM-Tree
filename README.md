# LSM tree

An implementation of the Log-Structured Merge Tree (LSM tree) data structure in Java.

**Table of Contents**

1. [Architecture](#Architecture)
    1. [SSTable](#SSTable)
    2. [Skip-List](#Skip-List)
    3. [Tree](#Tree)
4. [Benchmarks](#Benchmarks)
    1. [SSTable](#sstable-1)
    2. [Skip-List](#skip-list-1)
    3. [Tree](#tree-1)
5. [Implementation status](#Implementation-status)

## Console

To interact with a toy tree you can use `./gradlew run -q` to spawn a console.

![console.png](misc%2Fconsole.png)

---

# Architecture

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

To locate an element, we start from the top level and move forward until we find an element greater than the one
we are looking for. Then we move down to the next level and repeat the process until we find the element.

Insertions, deletions, and updates are done by first locating the element, then performing
the operation on the node. All of them have an average time complexity of `O(log(n))`.

---

## Tree

...

### Components

...

### Insertion

...

### Lookup

...

### Write-ahead logging

...

---

# Benchmarks

I am using [JMH](https://openjdk.java.net/projects/code-tools/jmh/) to run benchmarks,
the results are obtained on AMD Ryzen™ 5 4600H with 16GB of RAM and 512GB SSD.

> Take those with a grain of salt, development is still in progress.

To run them use `./gradlew jmh`.

## SSTable

- Negative access: the key is not present in the table, hence the Bloom filter will likely stop the search;
- Random access: the key is present in the table, the order of the keys is random.

```

Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.sstable.SSTableBenchmark.negativeAccess  thrpt    5  3316202.976 ±  32851.546  ops/s
c.t.l.sstable.SSTableBenchmark.randomAccess    thrpt    5     7989.945 ±     40.689  ops/s

```

## Bloom filter

- Add: add keys to a 1M keys Bloom filter with 0.01 false positive rate;
- Contains: test whether the keys are present in the Bloom filter.

```
Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.bloom.BloomFilterBenchmark.add           thrpt    5  3190753.307 ±  74744.764  ops/s
c.t.l.bloom.BloomFilterBenchmark.contains      thrpt    5  3567392.634 ± 220377.613  ops/s

```

## Skip-List

- Get: get keys from a 100k keys skip-list;
- Add/Remove: add and remove keys from a 100k keys skip-list.

```

Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.memtable.SkipListBenchmark.addRemove     thrpt    5   430239.471 ±   4825.990  ops/s
c.t.l.memtable.SkipListBenchmark.get           thrpt    5   487265.620 ±   8201.227  ops/s

```

## Tree

- Get: get elements from a tree with 1M keys;
- Add: add 1M distinct elements to a tree with a memtable size of 2^18

```
Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.tree.LSMTreeAddBenchmark.add             thrpt    5   540788.751 ±  54491.134  ops/s
c.t.l.tree.LSMTreeGetBenchmark.get             thrpt    5     9426.951 ±    241.190  ops/s

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
- [x] Skip-List
    - [x] Operations
    - [x] Iterator
- [x] Tree
    - [x] Operations
    - [x] Background flush
    - [x] Background compaction
    - [ ] Write ahead log
- [x] Benchmarks
    - [x] SSTable
    - [x] Bloom filter
    - [x] Skip-List
    - [x] Tree
