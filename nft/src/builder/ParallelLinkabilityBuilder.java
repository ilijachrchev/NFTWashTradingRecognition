package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;


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
        this.threadCount = Runtime.getRuntime().availableProcessors();
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

        // put the traders in an array so we can split them by index
        int[] traders = new int[traderIds.size()];
        int index = 0;
        for (int id : traderIds) {
            traders[index++] = id;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("from,to,weight\n");

            // start one worker per thread, each takes a share of the traders
            BfsWorker[] workers = new BfsWorker[threadCount];
            for (int i = 0; i < threadCount; i++) {
                workers[i] = new BfsWorker(i, traders, isTrader, writer);
                workers[i].start();
            }

            // wait for all of them to finish
            for (BfsWorker worker : workers) {
                worker.join();
            }

            // add up the counts from every worker
            long totalLinks = 0;
            long[] linksByWeight = new long[maxDepth + 1];
            for (BfsWorker worker : workers) {
                for (int i = 1; i <= maxDepth; i++) {
                    linksByWeight[i] += worker.linksByWeight[i];
                }
                totalLinks += worker.found;
            }

            Logger.debug("Processed " + traders.length + " / " + traders.length + " traders, found " + totalLinks + " links");

            StringBuilder distribution = new StringBuilder("Links by weight: ");
            for (int i = 1; i <= maxDepth; i++) {
                if (i > 1) {
                    distribution.append(", ");
                }
                distribution.append(" w=").append(i).append(": ").append(linksByWeight[i]);
            }
            Logger.info(distribution.toString());
        }
    }

    // one worker thread
    private final class BfsWorker extends Thread {

        private final int threadId;
        private final int[] traders;
        private final boolean[] isTrader;
        private final BufferedWriter writer;
        private final long[] linksByWeight;
        private long found;

        private BfsWorker(int threadId, int[] traders, boolean[] isTrader, BufferedWriter writer) {
            this.threadId = threadId;
            this.traders = traders;
            this.isTrader = isTrader;
            this.writer = writer;
            this.linksByWeight = new long[maxDepth + 1];
        }

        @Override
        public void run() {
            // the three main arrays, reused by this thread for every trader it handles
            int[] seen = new int[nodeCount];
            int[] distance = new int[nodeCount];
            int[] queue = new int[nodeCount];
            int bfsId = 0;

            for (int index = threadId; index < traders.length; index += threadCount) {
                int src = traders[index];
                bfsId++;

                int head = 0;
                int tail = 0;

                queue[tail++] = src;
                seen[src] = bfsId;
                distance[src] = 0;

                while (head < tail) {
                    int current = queue[head++];
                    int currentDistance = distance[current];

                    if (currentDistance >= maxDepth) {
                        continue;
                    }

                    List<Integer> neigh = etnGraph.getConnected(current);
                    for (int neighId : neigh) {
                        if (seen[neighId] == bfsId) {
                            continue;
                        }

                        int nextDistance = currentDistance + 1;
                        seen[neighId] = bfsId;
                        distance[neighId] = nextDistance;

                        queue[tail++] = neighId;

                        if (isTrader[neighId] && neighId != src) {
                            try {
                                synchronized (writer) {
                                    writer.write(src + "," + neighId + "," + nextDistance + "\n");
                                }
                            } catch (IOException e) {
                                Logger.error("Writer failed");
                            }

                            found++;
                            linksByWeight[nextDistance]++;
                        }
                    }
                }
            }
            Logger.info("[thread " + threadId + "] finished, found " + found + " links.");
        }
    }
}