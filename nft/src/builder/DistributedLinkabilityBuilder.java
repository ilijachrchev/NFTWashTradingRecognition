package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DistributedLinkabilityBuilder {

    private final int[] offsets;
    private final int[] neighbours;
    private final int[] allTraders;
    private final int maxDepth;
    private final int nodeCount;
    private final int rank;
    private final int size;

    public DistributedLinkabilityBuilder(int[] offsets, int[] neighbours, int[] allTraders, int maxDepth, int nodeCount, int rank, int size) {
        this.offsets = offsets;
        this.neighbours = neighbours;
        this.allTraders = allTraders;
        this.maxDepth = maxDepth;
        this.nodeCount = nodeCount;
        this.rank = rank;
        this.size = size;
    }

    // run bfs, save the edges and return how many links found for each wieght
    public long[] buildLocalPart(String tempFile) throws IOException {
        Logger.info("[rank " + rank + "] Building DISTRIBUTED linkability network with depth= " + maxDepth) ;

        boolean[] isTrader = new boolean[nodeCount];
        for (int id : allTraders) {
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        }

        long[] localLinksByWeight = new long[maxDepth + 1];
        long localTotal = 0;
        int[] seen  = new int[nodeCount];
        int[] distance = new int[nodeCount];
        int[] queue = new int[nodeCount];
        int bfsId = 1;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            if (rank == 0) {
                writer.write("from,to,weight\n");
            }

            int processed = 0;

            for (int index = rank; index < allTraders.length; index += size) {
                int source = allTraders[index];
                if (source < 0 || source >= nodeCount) {
                    continue;
                }
                bfsId++;
                int head = 0;
                int tail = 0;

                queue[tail++] = source;
                seen[source] = bfsId;
                distance[source] = 0;

                while (head < tail) {
                    int current = queue[head++];
                    int currentDistance = distance[current];

                    if (currentDistance >= maxDepth) {
                        continue;
                    }

                    int start = offsets[current];
                    int end = offsets[current+1];
                    for (int i = start; i < end; i++) {
                        int neighbourId = neighbours[i];
                        if (neighbourId < 0 || neighbourId >= nodeCount) {
                            continue;
                        }

                        if (seen[neighbourId] == bfsId) {
                            continue;
                        }

                        int nextDistance = currentDistance + 1;
                        seen[neighbourId] = bfsId;
                        distance[neighbourId] = nextDistance;

                        queue[tail++] = neighbourId;


                        if (isTrader[neighbourId] && neighbourId != source) {
                            writer.write(source + "," + neighbourId + "," + nextDistance + "\n");

                            localTotal++;
                            localLinksByWeight[nextDistance]++;
                        }
                    }
                }

                processed++;
                if (processed % 4000 == 0) {
                    Logger.debug("[rank " + rank + "] processed " + processed + ", found " + localTotal + " links." ) ;
                }
            }

            Logger.info("[rank " + rank + "] finished: " + processed + " traders processed, " + localTotal + " local links." ) ;
        }
        return localLinksByWeight;
    }
}
