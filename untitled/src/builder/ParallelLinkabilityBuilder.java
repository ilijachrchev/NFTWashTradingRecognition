package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
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
        this.threadCount = Runtime.getRuntime().availableProcessors();
    }

    public void buildLinkabilityNetwork(String outputFile) throws IOException {
        Logger.info("Building linkability network (PARALLEL) with max depth: " + maxDepth + ", using " + threadCount + " threads");
        Logger.info("Running BFS from: " + traderIds.size() + " NFT traders");
        Logger.info("Writting results to " + outputFile);

        boolean[] isTrader = new boolean[nodeCount];
        for (int id : traderIds) {
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        }

        ThreadLocal<int[]> localSeen = ThreadLocal.withInitial(() -> new int[nodeCount]);
        ThreadLocal<int[]> localDistance = ThreadLocal.withInitial(() -> new int[nodeCount]);
        ThreadLocal<int[]> localQueue = ThreadLocal.withInitial(() -> new int[Math.max(16, nodeCount/64)]);

        AtomicLong totalLinks = new AtomicLong(0);
        AtomicLong[] linksByWeight = new AtomicLong[maxDepth + 1];
        for (int i = 0; i <= maxDepth; i++) {
            linksByWeight[i] = new AtomicLong(0);
        }

        BlockingDeque<String> writeQueue = new LinkedBlockingDeque<>(200_000);
        String POISON = "__DONE__";

        AtomicLong processed = new AtomicLong(0);
        int totalTraders = traderIds.size();

        Thread writerThread = new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write("from,to,weight\n");
                while (true) {
                    String line = writeQueue.take();
                    if (line == POISON) break;
                    writer.write(line);
                }
            } catch ()
        })
        })

    }
}
