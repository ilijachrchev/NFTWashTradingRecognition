package builder;

import model.Graph;
import utils.Logger;

public class DistributedLinkabilityBuilder {

    private final Graph etnGraph;
    private final int[] allTraders;
    private final int maxDepth;
    private final int nodeCount;
    private final int rank;
    private final int size;

    public DistributedLinkabilityBuilder(Graph etnGraph, int[] allTraders, int maxDepth, int nodeCount, int rank, int size) {
        this.etnGraph = etnGraph;
        this.allTraders = allTraders;
        this.maxDepth = maxDepth;
        this.nodeCount = nodeCount;
        this.rank = rank;
        this.size = size;
    }

    // run bfs, save the edges and return how many links found for each wieght
    public long[] buildLocalPart(String tempFile) {
        Logger.info("[rank " + rank + "] Building DISTRIBUTED linkability network with depth= " + maxDepth) ;

        boolean[] isTrader = new boolean[nodeCount];
        for (int id : allTraders) {
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        }

        long[] localLinksByWeight = new long[nodeCount];
        long localTotal = 0;
    }
}
