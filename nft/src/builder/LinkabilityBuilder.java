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
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        }

        int[] seen = new int[nodeCount];
        int[] distance = new int[nodeCount];
        int[] queue = new int[nodeCount];
        int bfsId = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("from,to,weight\n");

            for (int source : traderIds) {
                if (source < 0 || source >= nodeCount) continue;

                bfsId++;
                int head = 0;
                int tail = 0;

                queue[tail++] = source;
                seen[source] = bfsId;
                distance[source] = 0;

                while (head < tail) {
                    int current = queue[head++];
                    int currentDistance = distance[current];

                    if (currentDistance >= maxDepth) continue;

                    List<Integer> neighbours = etnGraph.getConnected(current);
                    for (int i = 0; i < neighbours.size(); i++) {
                        int neighbour = neighbours.get(i);

                        if (neighbour < 0 || neighbour >= nodeCount) continue;
                        if (seen[neighbour] == bfsId) continue;

                        int nieghbourDistance = currentDistance + 1;
                        seen[neighbour] = bfsId;
                        distance[neighbour] = nieghbourDistance;

                        queue[tail++] = neighbour;

                        if (isTrader[neighbour] && neighbour != source) {
                            writer.write(source + "," + neighbour + "," + nieghbourDistance + "\n");
                            totalLinks++;
                            linksByWeight[nieghbourDistance]++;
                        }
                    }
                }

                processed++;
                if (processed % 4000 == 0) {
                    Logger.debug("Processed " + processed + " /" + traderIds.size() + " traders, found " + totalLinks + " links!");
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
}
