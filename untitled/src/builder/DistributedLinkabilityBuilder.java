package builder;

import model.Graph;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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
    public long[] buildLocalPart(String tempFile) throws IOException {
        Logger.info("[rank " + rank + "] Building DISTRIBUTED linkability network with depth= " + maxDepth) ;

        boolean[] isTrader = new boolean[nodeCount];
        for (int id : allTraders) {
            if (id >= 0 && id < nodeCount) {
                isTrader[id] = true;
            }
        }

        long[] localLinksByWeight = new long[nodeCount];
        long localTotal = 0;
        int[] seen  = new int[nodeCount];
        int[] distance = new int[nodeCount];
        int[] queue = new int[Math.max(16, nodeCount / 64)];
        int bfsId = 1;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            if (rank == 0) {
                writer.write("from,to,weight\n");
            }

            int processed = 0;
            int myShare = 0;

            for (int index = rank; index < allTraders.length; index += size) {
                myShare++;
                int source = allTraders[index];
                if (source < 0 || source >= nodeCount) {
                    continue;
                }
                int head = 0;
                int tail = 0;

                if (tail == queue.length) {
                    queue = grow(queue);
                }

                queue[tail++] = source;
                seen[source] = bfsId;
                distance[source] = 0;

                while (head < tail) {
                    int current = queue[head++];
                    int currentDistance = distance[current];

                    if (currentDistance >= maxDepth) {
                        continue;
                    }

                    Graph.IntVec neigh = etnGraph.getConnected(current);
                    for (int i = 0; i < neigh.size(); i++) {
                        int neighId = neigh.get(i);
                        if (neighId < 0 || neighId >= nodeCount) {
                            continue;
                        }

                        if (seen[neighId] == bfsId) {
                            continue;
                        }

                        int nextDistance = currentDistance + 1;
                        seen[neighId] = bfsId;
                        distance[neighId] = nextDistance;

                        if (tail == queue.length) {
                            queue = grow(queue);
                        }
                        queue[tail++] = neighId;


                        if (isTrader[neighId] && neighId != source) {
                            writer.write(neighId + ", " + nextDistance + ", " + "\n");

                            localTotal++;
                            localLinksByWeight[nextDistance]++;
                        }
                    }
                }

                processed++;
                if (processed %4000 == 0) {
                    Logger.debug("[rank " + rank + "] processed " + processed + ", found " + localTotal + " links." ) ;
                }

                bfsId++;
                if (bfsId == Integer.MAX_VALUE) {
                    for (int i = 0; i < nodeCount; i++) {
                        seen[i] = 0;
                    }
                    bfsId = 1;
                }
            }

            Logger.info("[rank " + rank + "] finished: " + myShare + " traders processed, " + localTotal + " local links." ) ;
        }
        return localLinksByWeight;
    }

    private static int[] grow(int[] array) {
        int[] bigger = new int[array.length * 2];
        System.arraycopy(array, 0, bigger, 0, array.length);
        return bigger;
    }
}
