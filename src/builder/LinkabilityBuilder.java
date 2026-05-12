package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LinkabilityBuilder {

    private final Graph etnGraph;
    private final Set<Integer> traderIds;
    private final int maxDepth;
    private final int nodeCount;

    public LinkabilityBuilder(Graph etnGraph, Set<Integer> traderIds, int maxDepth, int nodeCount) {
        this.etnGraph = etnGraph;
        this.traderIds = traderIds;
        this.maxDepth = maxDepth;
        this.nodeCount = nodeCount;
    }

    public void buildLinkabilityNetwork(String outputFile) throws IOException {
        Logger.info("Building linkability network with max depth " + maxDepth);
        Logger.info("Running BFS from " + traderIds.size() + " NFT traders");
        Logger.info("Writing results to: " + outputFile);

        long totalLinks = 0;
        int processed = 0;
        long[] linksByWeight = new long[maxDepth + 1];
        boolean[] isTrader = new boolean[nodeCount];

        for (int id : traderIds) {
            if (id >= 0 && id < nodeCount) isTrader[id] = true;
        }

        int[] seen = new int[nodeCount];
        int[] distance = new int[nodeCount];
        int[] queue = new int[Math.max(16, nodeCount / 64)];
        int bfsId = 1;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("from,to,weight\n");

            for (int source : traderIds) {
                if (source < 0 || source >= nodeCount) continue;

                int head = 0;
                int tail = 0;

                if (tail == queue.length) queue = grow(queue);
                queue[tail++] = source;
                seen[source] = bfsId;
                distance[source] = 0;

                while (head < tail) {
                    int current = queue[head++];
                    int currentDistance = distance[current];

                    if (currentDistance >= maxDepth) continue;

                    Graph.IntVec neigh = etnGraph.getConnected(current);
                    for (int i = 0; i < neigh.size(); i++) {
                        int nb = neigh.get(i);

                        if (nb < 0 || nb >= nodeCount) continue;
                        if (seen[nb] == bfsId) continue;

                        int nd = currentDistance + 1;
                        seen[nb] = bfsId;
                        distance[nb] = nd;

                        if (tail == queue.length) queue = grow(queue);
                        queue[tail++] = nb;

                        if (isTrader[nb] && nb != source) {
                            writer.write(source + "," + nb + "," + nd + "\n");
                            totalLinks++;
                            linksByWeight[nd]++;
                        }
                    }
                }

                processed++;
                if (processed % 4000 == 0) {
                    Logger.debug("Processed " + processed + " /" + traderIds.size() + " traders, found " + totalLinks + " links!");
                }

                bfsId++;
                if (bfsId == Integer.MAX_VALUE) {
                    for (int i = 0; i < nodeCount; i ++) seen[i] = 0;
                    bfsId = 1;
                }
            }
            Logger.debug("Processed " + processed + " /" + traderIds.size() + " traders, found " + totalLinks + " links!");
        }

        StringBuilder distribution = new StringBuilder("Links by weight:");
        for (int i = 1; i <= maxDepth; i++){
            if (i > 1) distribution.append(",");
            distribution.append(" w=").append(i).append(": ").append(linksByWeight[i]);
        }
        Logger.info(distribution.toString());
    }

    private static int[] grow(int[] array) {
        int[] newArray = new int[array.length * 2];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }
}
