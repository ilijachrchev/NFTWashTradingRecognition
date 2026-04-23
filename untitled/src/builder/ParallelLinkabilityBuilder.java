package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ParallelLinkabilityBuilder {

    private final Graph etnGraph;
    private final Set<Integer> traderIds;
    private final int maxDepth;
    private final int nodeCount;
    private final int threadCount;

    public ParallelLinkabilityBuilder(Graph etnGraph, Set<Integer> traderIds, int maxDepth, int nodeCount) {
        this.etnGraph = etnGraph;
        this.traderIds = traderIds;
        this.maxDepth = maxDepth;
        this.nodeCount = nodeCount;
        this.threadCount = Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    public void buildLinkabilityNetwork(String outputFile) throws IOException, InterruptedException {
        Logger.info("Building linkability network (PARALLEL) with a max depth of: " + maxDepth);
        Logger.info("Using " + threadCount + " threads");
        Logger.info("Running BFS from: " + traderIds.size() + " NFT traders");
        Logger.info("Writing results to: " + outputFile);

        boolean[] isTrader = new boolean[nodeCount];
        for (int id : traderIds) {
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        } // build fast lookup table, later access it in O(1) with isTrader[variable]

        //safe updates with atomic
        AtomicLong totalLinks = new AtomicLong(0);
        AtomicLong[] linksByWeight = new AtomicLong[maxDepth + 1];
        for (int i = 0; i <= maxDepth; i++) {
            linksByWeight[i] = new AtomicLong(0);
        }

        BlockingQueue<String> writingQueue = new ArrayBlockingQueue<>(8192);
        final String POISON = "__POISON__";

        Thread writerThread = new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write("from,to,weight\n");

                while (true) {
                    String chunk = writingQueue.take();
                    if (chunk.equals(POISON)) {
                        break;
                    }
                    writer.write(chunk);
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.error("Writer thread failed: " + e.getMessage());
            }
        }, "csv-writer");

        writerThread.start();

        // the three main arrays initialized in bfsworkspace, reused by same thread
        ThreadLocal<BfsWorkspace> workspaceLocal = ThreadLocal.withInitial(()
                -> new BfsWorkspace(nodeCount));

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger processed = new AtomicInteger(0);
        int totalTraders = traderIds.size();

        for (int source : traderIds) {
            final int src = source;

            pool.submit(() -> {
                if (src < 0 || src >= nodeCount) {
                    return;
                }

                BfsWorkspace workspace = workspaceLocal.get();
                int bfsId = workspace.nextBfsId();

                StringBuilder localOutput = new StringBuilder(1024);
                long localLinks = 0;
                long[] localLinksByWeight = new long[maxDepth + 1];

                int head = 0;
                int tail = 0;

                workspace.ensureQueueCapacity(1);
                workspace.queue[tail++] = src;
                workspace.seen[src] = bfsId;
                workspace.distance[src] = 0;

                while (head < tail) {
                    int current = workspace.queue[head++];
                    int currentDistance = workspace.distance[current];

                    if (currentDistance >= maxDepth) {
                        continue;
                    }

                    Graph.IntVec neigh = etnGraph.getConnected(current);
                    for (int i = 0; i < neigh.size(); i++) {
                        int neighId = neigh.get(i);
                        if (neighId < 0 || neighId >= nodeCount) {
                            continue;
                        }

                        if (workspace.seen[neighId] == bfsId) {
                            continue;
                        }

                        int nextDistance = currentDistance + 1;
                        workspace.seen[neighId] = bfsId;
                        workspace.distance[neighId] = nextDistance;

                        workspace.ensureQueueCapacity(tail + 1);
                        workspace.queue[tail++] = neighId;

                        if (isTrader[neighId] && neighId != src) {
                            localOutput.append(src)
                                    .append(",")
                                    .append(neighId)
                                    .append(",")
                                    .append(nextDistance)
                                    .append('\n');

                            localLinks++;
                            localLinksByWeight[nextDistance]++;
                        }
                    }
                }
                if (localOutput.length() > 0) {
                    try {
                        writingQueue.put(localOutput.toString());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                totalLinks.addAndGet(localLinks);
                for (int i = 1; i <= maxDepth; i++) {
                    if (localLinksByWeight[i] != 0) {
                        linksByWeight[i].addAndGet(localLinksByWeight[i]);
                    }
                }

                int finished = processed.incrementAndGet();
                if (finished % 4000 == 0) {
                    Logger.debug("Processed " + finished + " / " + totalTraders + " traders, found " + totalLinks.get() + " links");
                }
            });
        }

        pool.shutdown();
        boolean finished = pool.awaitTermination(365, TimeUnit.DAYS);

        if (!finished) {
            pool.shutdownNow();
            throw new RuntimeException("Worker pool did not terminate cleanly.");
        }

        writingQueue.put(POISON);
        writerThread.join();

        Logger.debug("Processed " + totalTraders + " / " + totalTraders + " traders, found " + totalLinks.get() + " links");

        StringBuilder distribution = new StringBuilder("Links by weight: ");
        for (int i = 1; i <= maxDepth; i++) {
            if (i > 1) {
                distribution.append(", ");
            }

            distribution.append(" w=").append(i).append(": ").append(linksByWeight[i].get());
        }
        Logger.info(distribution.toString());
    }

    private static final class BfsWorkspace {

        private final int[] seen;
        private final int[] distance;
        private int[] queue;
        private int bfsId;

        private BfsWorkspace(int nodeCount) {
            this.seen = new int[nodeCount];
            this.distance = new int[nodeCount];
            this.queue = new int[Math.max(16, nodeCount / 64)];
            this.bfsId = 1;
        }

        private int nextBfsId() {
            bfsId++;

            if (bfsId == Integer.MAX_VALUE) {
                for (int i = 0; i < seen.length; i++) {
                    seen[i] = 0;
                }
                bfsId = 1;
            }

            return bfsId;
        }

        private void ensureQueueCapacity(int needed) {
            if (needed <= queue.length) {
                return;
            }

            int newCapacity = queue.length;
            while (newCapacity < needed) {
                newCapacity *= 2;
            }

            int[] bigger = new int[newCapacity];
            System.arraycopy(queue, 0, bigger, 0, queue.length);
            queue = bigger;
        }
    }




}
