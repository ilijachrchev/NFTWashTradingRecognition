import builder.DistributedLinkabilityBuilder;
import builder.GraphBuilder;
import builder.NFTTraderLoader;
import model.Graph;
import mpi.MPI;
import utils.BlacklistReader;
import utils.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class DistributedMain {

    private static final int MAX_DEPTH = 2;
    private static final String ETN_FILE = "";
    private static final String NFT_FILE = "";
    private static final String BLACKLIST_FOLDER = "";
    private static final String OUTPUT_FILE = "";
    // or through example. 3 data/prog3ETNsample.csv data/boredapeyachtclub.csv blacklist data/output_distributed.csv

    public static void main(String[] args) throws IOException {
       String[] userArgs = MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        long totalStartTime = System.currentTimeMillis();


        int maxDepth;
        String etnFile, nftFile, blacklistFolder, outputFile;

        if (userArgs.length>= 5) {
            maxDepth = Integer.parseInt(userArgs[0]);
            etnFile = userArgs[1];
            nftFile = userArgs[2];
            blacklistFolder = userArgs[3];
            outputFile = userArgs[4];
        } else {
            maxDepth = MAX_DEPTH;
            etnFile = ETN_FILE;
            nftFile = NFT_FILE;
            blacklistFolder = BLACKLIST_FOLDER;
            outputFile = OUTPUT_FILE;
        }

        if (rank == 0) {
            Logger.info("==================== Wash Trading Detection - Distributed ====================");
            Logger.info("Max depth: " + maxDepth);
            Logger.info("ETN file: " + etnFile);
            Logger.info("NFT file: " + nftFile);
            Logger.info("Blacklist folder: " + blacklistFolder);
            Logger.info("Output file: " + outputFile);
            Logger.info("=============================================================================");
        }

        Graph graph;
        int[] traders;
        int nodeCount;

        if (rank == 0) {
            Set<String> blacklist = BlacklistReader.loadBlacklist(blacklistFolder);

            GraphBuilder builder = new GraphBuilder(blacklist);
            builder.buildFromETN(etnFile);
            graph = builder.getGraph();
            graph.duplicateEdges();

            NFTTraderLoader nftLoader = new NFTTraderLoader(blacklist, builder.getAddressMapper());
            Set<Integer> traderSet = nftLoader.loadTraders(nftFile);
            traders = traderSet.stream().mapToInt(Integer::intValue).sorted().toArray();
            nodeCount = builder.getAddressMapper().size();

            // memory prob without this
            builder.getAddressMapper().clear(); // after graph and trader ids are built, i dont need
            blacklist.clear(); // blacklist is also not needed after graph and nft trader loading

            int[][] flat =  flattenGraph(graph, nodeCount);
            int[] offsets = flat[0];
            int[] neighbors = flat[1];

            int[] sizes = new int[3];
            sizes[0] = nodeCount;
            sizes[1] = neighbors.length;
            sizes[2] = traders.length;

            MPI.COMM_WORLD.Bcast(sizes, 0, 3, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(offsets, 0, offsets.length, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(neighbors, 0, neighbors.length, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(traders, 0, traders.length, MPI.INT, 0);

            Logger.success("Graph loaded: " + nodeCount + " nodes, " + graph.edgeCount() + " edges, " + traders.length + " traders");
        } else {
            int[] sizes = new int[3];
            MPI.COMM_WORLD.Bcast(sizes, 0, 3, MPI.INT, 0);
            nodeCount = sizes[0];
            int edgeCount = sizes[1];
            int traderCount = sizes[2];

            int[] offsets = new int[nodeCount + 1];
            int[] neighbors = new int[edgeCount];
            traders = new int[traderCount];

            MPI.COMM_WORLD.Bcast(offsets, 0, offsets.length, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(neighbors, 0, neighbors.length, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(traders, 0, traders.length, MPI.INT, 0);

            graph = rebuildGraph(offsets, neighbors, nodeCount);
        }

        MPI.COMM_WORLD.Barrier();
        double tStart = MPI.Wtime();
        long tStartMs = System.currentTimeMillis();

        String tempFile = outputFile + ".rank" + rank + ".tmp"; // each rank gets its own temp file named by rank
        DistributedLinkabilityBuilder distributedBuilder = new DistributedLinkabilityBuilder(graph, traders, maxDepth, nodeCount, rank, size);
        long[] localCounts = distributedBuilder.buildLocalPart(tempFile);

        MPI.COMM_WORLD.Barrier();
        double tEnd = MPI.Wtime();
        long tEndMs = System.currentTimeMillis();

        long[] globalCounts = new long[maxDepth + 1];
        MPI.COMM_WORLD.Reduce(localCounts, 0, globalCounts, 0, maxDepth + 1, MPI.LONG, MPI.SUM, 0);

        if (rank == 0) {
            mergeFiles(outputFile, size);

            long total = 0;
            StringBuilder distance = new StringBuilder("Link by weight:");
            for (int i = 1; i <= maxDepth; i++) {
                if (i > 1) {
                    distance.append(",");
                }
                distance.append(" w=").append(i).append(": ").append(globalCounts[i]);
                total += globalCounts[i];
            }

            Logger.info(distance.toString());
            Logger.success("Total links: " + total);
            Logger.success("Output written to: " + outputFile);

            long totalTime = System.currentTimeMillis() - totalStartTime;
            Logger.success("TOTAL RUNTIME: " + totalTime + " ms (" + (totalTime / 1000.0) + " seconds)");

            long bfsMs = tEndMs - tStartMs;
            Logger.success("BFS computation runtime: " + bfsMs + " ms");
        }


        MPI.Finalize();
    }

    public static void mergeFiles(String outputFile, int size) throws IOException {
        Path out = Path.of(outputFile);
        Files.deleteIfExists(out);

        try (var output = Files.newOutputStream(out)){
            for (int i = 0; i <size; i++) {
                Path temp = Path.of(outputFile + ".rank" + i + ".tmp");

                if (!Files.exists(temp)) {
                    continue;
                }

                Files.copy(temp, output);
                Files.delete(temp);
            }
        }
    }

    // make two int[] arrays and for braodcasting
    private static int[][] flattenGraph(Graph graph, int nodeCount) {
        int[] offsets = new int[nodeCount + 1];
        int total = 0;
        for (int i = 0; i < nodeCount; i++) {
            offsets[i] = total;
            total += graph.getConnected(i).size();
        }
        offsets[nodeCount] = total;

        int[] neighbors = new int[total];
        int position = 0;
        for (int i = 0; i < nodeCount; i++) {
            Graph.IntVec v = graph.getConnected(i);
            for (int j = 0; j < v.size(); j++) {
                neighbors[position++] = v.get(j);
            }
        }
        return new int[][]{offsets, neighbors};
    }

    // reverse from flattenGraph
    private static Graph rebuildGraph(int[] offsets, int[] neighbors, int nodeCount) {
        Graph g = new Graph();

        for (int i = 0; i < nodeCount; i++) {
            int start = offsets[i];
            int end = offsets[i + 1];
            for (int j = start; j < end; j++) {
                g.addEdge(i, neighbors[j]);
            }
        }
        return g;
    }
}
