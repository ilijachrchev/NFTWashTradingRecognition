# Transaction Network Analysis

Detects possible NFT wash trading by building a linkability network from the
Ethereum Transaction Network. Runs a bounded BFS from every Bored Ape Yacht Club
trader and reports every pair of traders connected within a given depth.

The same algorithm is implemented three times: sequential, parallel and
distributed with MPI.

## Requirements

* Java 21
* MPJ Express v0.44 (only for distributed mode)

## Setup

1. Open the project in IntelliJ.
2. Set the `MPJ_HOME` environment variable to your MPJ Express install, for example `C:\MPJ\mpj-v0_44`.
3. Add MPJ jars to the project: `File` -> `Project Structure` -> `Libraries` -> `+` -> `Java`, select all `.jar` files in `$MPJ_HOME/lib`.
4. Set the working directory of every run configuration to the folder that contains `data/` and `blacklist/`.

## How to run

### Sequential

Open `SequentialMain.java` and run it. No extra setup needed.

| Field | Value |
| --- | --- |
| Main class | `SequentialMain` |

### Parallel

Open `ParallelMain.java` and run it. Uses all available CPU threads automatically.

| Field | Value |
| --- | --- |
| Main class | `ParallelMain` |

### Distributed

Create a Run Configuration with these settings:

| Field | Value |
| --- | --- |
| Main class | `DistributedMain` |
| VM options | `-jar $MPJ_HOME$/lib/starter.jar -np 8` |
| Working directory | folder containing `data/` and `blacklist/` |
| Environment variables | `MPJ_HOME=C:\MPJ\mpj-v0_44` |

Change `-np N` to set the number of processes.

### Optional program arguments

All three modes accept the same arguments:

    <max_depth> <etn_file> <nft_file> <blacklist_folder> <output_file>

`Configuration` -> `Program Arguments`, for example:

    7 data/prog3ETNsample.csv data/boredapeyachtclub.csv blacklist data/output.csv

If omitted, the defaults defined in the entry point class are used.

## Output

A CSV with one line per linkable pair:

    from,to,weight

`from` and `to` are internal integer IDs of the two traders, and `weight` is the
BFS distance between them.

## How it works

Every mode runs the same four steps. Load the blacklist, build the transaction
graph from the ETN CSV, load the BAYC traders, then run a bounded BFS from every
trader and write one line per linkable pair.

Addresses are mapped to integer IDs on the way in, so the graph and the BFS work
on primitive ints instead of 42 character strings.

Each BFS reuses the same `seen`, `distance` and `queue` arrays. Instead of
clearing them between traders, every traversal gets its own stamp number and a
node counts as visited only if its stamp matches the current one.

**Sequential** runs the traders one after another on a single thread.

**Parallel** starts one worker thread per available processor. The traders are
put in an array and split by interleaved index, so worker `t` handles indices
`t`, `t+threadCount`, `t+2*threadCount` and so on. Each worker keeps its own
`seen`, `distance` and `queue` arrays, so no two threads touch the same BFS
state. All workers share one writer, and every write goes inside a
`synchronized` block. Each worker counts its own links, and the main thread adds
them up once all workers have finished.

**Distributed** uses MPJ Express. Rank 0 reads the file and flattens the graph
into two integer arrays, one with the start index of each node's neighbours and
one with all neighbours in order, then broadcasts them so the other ranks do not
have to read the 5 GB file themselves. Traders are split by interleaved index, so
rank `r` takes indices `r`, `r+size`, `r+2*size` and so on. Each rank writes its
own temporary file and rank 0 merges them at the end.

## Performance

Test machine: AMD Ryzen 7 7840HS (8 cores, 16 threads), 16 GB RAM, Java 21.
Dataset: 5 GB ETN CSV, 5,016,686 nodes and 5,943,986 edges after blacklist
filtering and duplicate removal, 19,714 NFT traders. All runs at depth 7.

Every configuration produced exactly 988,249 links with the same breakdown per
weight, so all three modes agree.

| Mode | Total |
| --- | --- |
| Sequential | 228.8 s |
| Parallel (16 threads) | 64.4 s |
| Distributed `-np 2` | 104.1 s |
| Distributed `-np 4` | 69.3 s |
| Distributed `-np 6` | 72.2 s |
| Distributed `-np 8` | 67.7 s |

### Observations

The gap between the two columns is the setup cost, and it stays between 39 and
47 seconds in every run. Reading and parsing the 5 GB file is sequential in all
three modes, so it never gets faster no matter how many threads or ranks are
used. This is why the totals converge even though the BFS times do not.

Parallel mode gives the shortest total runtime. All 16 threads share one graph in
memory, so it is stored once and there is nothing to broadcast.

Distributed mode scales well on the BFS phase, going from 57.8 s at 2 ranks to
21.1 s at 8 ranks. It still cannot beat parallel mode here, because rank 0 has to
read the file and broadcast the graph before any BFS can start. On a real cluster
each node would read its own share in parallel and that cost would drop.

The distributed BFS at 2 ranks is already much faster than the sequential BFS.
That is not scaling, it is the data structure. The distributed version walks two
flat `int[]` arrays, while the sequential one walks a `HashMap` of
`ArrayList<Integer>`, which costs a hash lookup and an unboxing step per
neighbour. The flat arrays exist for broadcasting, but they made the traversal
faster too.