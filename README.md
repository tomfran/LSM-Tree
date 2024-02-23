# LSM tree

An implementation of the Log-Structured Merge Tree (LSM tree) data structure in Java.

Here you can find a [Medium article](https://medium.com/@tomfran/log-structured-merge-tree-a79241c959e3) about this project.

**Table of Contents**

1. [Architecture](#architecture)
    1. [SSTable](#sstable)
    2. [Skip-List](#skip-list)
    3. [Tree](#tree)
4. [Benchmarks](#benchmarks)
    1. [SSTable](#sstable-1)
    2. [Skip-List](#skip-list-1)
    3. [Tree](#tree-1)
5. [Possible future improvements](#possible-improvements)
6. [References](#references)

### Console

To interact with a toy tree you can use `./gradlew run -q` to spawn a console.

```

  |      __|   \  |           __ __|              
  |    \__ \  |\/ |   ____|      |   _| -_)   -_) 
 ____| ____/ _|  _|             _| _| \___| \___| 

Commands:
  - s/set  <key> <value> : insert a key-value pair;
  - r/rgn  <start> <end> : insert this range of numeric keys with random values;
  - g/get  <key>         : get a key value;
  - d/del  <key>         : delete a key;
  - p/prt                : print current tree status;
  - e/exit               : stop the console;
  - h/help               : show this message.

> 
```

# Architecture

Architecture overview, from SSTables, which are the disk-resident portion of the database, Skip Lists, used
as memory buffers, and finally to the combination of the twos to create insertion, lookup and deletion primitives.

## SSTable

Sorted String Table (SSTable) is a collection of files modelling key-value pairs in sorted order by key.
It is used as a persistent storage for the LSM tree.

**Components**

- _Data_: key-value pairs in sorted order by key, stored in a file;
- _Sparse index_: sparse index containing key and offset of the corresponding key-value pair in the data;
- _Bloom filter_: a [probabilistic data structure](https://en.wikipedia.org/wiki/Bloom_filter) used to test whether a
  key is in the SSTable.

**Key lookup**

The basic idea is to use the sparse index to find the key-value pair in the data file.
The steps are:

1. Use the Bloom filter to test whether the key might be in the table;
2. If the key might be present, use binary search on the index to find the maximum lower bound of the key;
3. Scan the data from the position found in the previous step to find the key-value pair. The search
   can stop when we are seeing a key greater than the one we are looking for, or when we reach the end of the table.

The search is as lazy as possible, meaning that we read the minimum amount of data from disk,
for instance, if the next key length is smaller than the one we are looking for, we can skip the whole key-value pair.

**Persistence**

A table is persisted to disk when it is created. A base filename is defined, and three files are present:

- `<base_filename>.data`: data file;
- `<base_filename>.index`: index file;
- `<base_filename>.bloom`: bloom filter file.

Data format:

- `n`: number of key-value pairs;
- `<key_len_1, value_len_1, key_1, value_1, ... key_n, value_n>`: key-value pairs.

Index format:

- `s`: number of entries in the whole table;
- `n`: number of entries in the index;
- `o_1, o_2 - o_1, ..., o_n - o_n-1`: offsets of the key-value pairs in the data file, skipping
  the first one;
- `s_1, s_2, ..., s_n`: remaining keys after a sparse index entry, used to exit from search;
- `<key_len_1, key_1, ... key_len_n, key_n>`: keys in the index.

Filter format:

- `m`: number of bits in the bloom filter;
- `k`: number of hash functions;
- `n`: size of underlying long array;
- `b_1, b_2, ..., b_n`: bits of the bloom filter.

To save space, all integers are stored
in [variable-length encoding](https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html),
and offsets in the index are stored as [deltas](https://en.wikipedia.org/wiki/Delta_encoding).

## Skip-List

A [skip-list](https://en.wikipedia.org/wiki/Skip_list) is a probabilistic data structure that allows fast search,
insertion and deletion of elements in a sorted sequence.

In the LSM tree, it is used as an in-memory data structure to store key-value pairs in sorted order by key.
Once the skip-list reaches a certain size, it is flushed to disk as an SSTable.

**Operations details**

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

## Tree

Having defined SSTables and Skip Lists we can obtain the final structure as a combination of the two.
The main idea is to use the latter as an in-memory buffer, while the former efficiently stores flushed
buffers.

**Insertion**

Each insert goes directly to a Memtable, which is a Skip List under the hood, so the response time is quite fast.
There exists a threshold, over which the mutable structure is made immutable by appending it to the _immmutable
memtables LIFO list_ and replaced with a new mutable list.

The immutable memtable list is asynchronously consumed by a background thread, which takes the next available
list and create a disk-resident SSTable with its content.

**Lookup**

While looking for a key, we proceed as follows:

1. Look into the in-memory buffer, if the key is recently written it is likely here, if not present continue;
2. Look into the immutable memtables list, iterating from the most recent to the oldest, if not present continue;
3. Look into disk tables, iterating from the most recent one to the oldest, if not present return null.

**Deletions**

To delete a key, we do not need to delete all its replicas, from the on-disk tables, we just need a special
value called _tombstone_. Hence a deletion is the same as an insertion, but with a value set to null. While looking for
a key, if we encounter a null value we simply return null as a result.

**SSTable Compaction**

The most expensive operation while looking for a key is certainly the disk search, and this is why bloom filters are
crucial for negative
lookup on SSTables. But no bloom filter can save us if too many tables are available to search, hence we need
_compaction_.

When flushing a Memtable, we create an SSTable of level zero.
When the first level reaches a certain threshold, all its tables are merged with 
the subsequent level in a sorted run.

A sorted run is a procedure in which we merge SSTables into multiple tables. The result 
is a sequence of SSTs that are non-intersecting, more details can be found in the Medium article.

This check is made periodically on all levels to ensure a level does not grow too much.
Levels and SST sizes increases by a factor of 1.75 on each step.

# Benchmarks

I am using [JMH](https://openjdk.java.net/projects/code-tools/jmh/) to run benchmarks,
the results are obtained on a base model M3 pro Macbook Pro.

To run them use `./gradlew jmh`.

**SSTable**

- Negative access: the key is not present in the table, hence the Bloom filter will likely stop the search;
- Random access: the key is present in the table, the order of the keys is random.

```

Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.sstable.SSTableBenchmark.negativeAccess  thrpt    5  3316202.976 ±  32851.546  ops/s
c.t.l.sstable.SSTableBenchmark.randomAccess    thrpt    5     7989.945 ±     40.689  ops/s

```

**Bloom filter**

- Add: add keys to a 1M keys Bloom filter with 0.01 false positive rate;
- Contains: test whether the keys are present in the Bloom filter.

```
Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.bloom.BloomFilterBenchmark.add        thrpt    5  10870782.166 ± 151949.254  ops/s
c.t.l.bloom.BloomFilterBenchmark.contains   thrpt    5  11061776.096 ±  16752.915  ops/s
```

**Skip-List**

- Get: get keys from a 100k keys skip-list;
- Add/Remove: add and remove keys from a 100k keys skip-list.

```
Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.memtable.SkipListBenchmark.addRemove  thrpt    5   1066479.961 ±  70216.252  ops/s
c.t.l.memtable.SkipListBenchmark.get        thrpt    5   1280680.984 ±  42235.970  ops/s
```

**Tree**

- Get: get elements from a tree with 1M keys;
- Add: add 1M distinct elements to a tree with a memtable size of 2^18

```
Benchmark                                       Mode  Cnt        Score        Error  Units
c.t.l.tree.LSMTreeAddBenchmark.add          thrpt    5    722278.306 ±  30802.444  ops/s
c.t.l.tree.LSMTreeGetBenchmark.get          thrpt    5     20098.919 ±    240.244  ops/s
```

## Possible improvements

There is certainly space for improvement on this project:

- [ ] Blocked bloom filters: its a variant of a classic array-like bloom filter which is more cache efficient;
- [ ] Search fingers in the Skip list: the idea is to keep a pointer to the last search, and start from there with
   subsequent queries;
- [x] Proper level compaction in the LSM tree;
- [ ] Write ahead log for the insertions, without this, a crash makes all the in-memory writes disappear;
- [ ] Proper recovery: handle crashes and reboots, using existing SSTables and the write-ahead log.

I don't have the practical time to do all of this, perhaps the first two points will be handled in the future.

## References

- [Database Internals](https://www.databass.dev/) by Alex Petrov, specifically chapters about Log-Structured Storage and
  File Formats;
- [A Skip List Cookbook](https://api.drum.lib.umd.edu/server/api/core/bitstreams/17176ef8-8330-4a6c-8b75-4cd18c570bec/content)
  by William Pugh.

_If you found this useful or interesting do not hesitate to ask clarifying questions or get in touch!_
