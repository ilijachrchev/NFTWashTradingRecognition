import builder.GraphBuilder;
import builder.NFTTraderLoader;
import builder.ParallelLinkabilityBuilder;
import model.Graph;
import utils.BlacklistReader;
import utils.Logger;

import java.io.IOException;
import java.util.Set;


public class ParallelMain {

    private static final int MAX_DEPTH = 9;
    private static final String ETN_FILE = "prog3ETNsample.csv";
    private static final String NFT_FILE = "boredapeyachtclub.csv";
    private static final String BLACKLIST_FOLDER = "blacklist";
    private static final String OUTPUT_FILE = "output_parallel.csv";

    public static void main(String[] args) {

        int maxDepth;
        String etnFile;
        String nftFile;
        String blacklistFolder;
        String outputFile;

        if (args.length >= 5) {
            try {
                maxDepth = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                Logger.error("Invalid max depth parameter: " + args[0]);
                printUsage();
                System.exit(1);
                return;
            }
            etnFile = args[1];
            nftFile = args[2];
            blacklistFolder = args[3];
            outputFile = args[4];
        } else {
            maxDepth = MAX_DEPTH;
            etnFile = ETN_FILE;
            nftFile = NFT_FILE;
            blacklistFolder = BLACKLIST_FOLDER;
            outputFile = OUTPUT_FILE;
        }

        if (maxDepth < 1 || maxDepth > 10) {
            Logger.error("Max depth must be between 1 and 10");
            System.exit(1);
        }

        Logger.info("==================== Wash Trading Detection - Parallel ====================");
        Logger.info("Threads: " + Runtime.getRuntime().availableProcessors());
        Logger.info("Max depth: " + maxDepth);
        Logger.info("ETN file: " + etnFile);
        Logger.info("NFT file: " + nftFile);
        Logger.info("Blacklist folder: " + blacklistFolder);
        Logger.info("Output file: " + outputFile);
        Logger.info("=============================================================================");

        long totalStartTime = System.currentTimeMillis();

        try {
            long startTime = System.currentTimeMillis();
            Set<String> blacklist = BlacklistReader.loadBlacklist(blacklistFolder);
            long blacklistTime = System.currentTimeMillis() - startTime;
            Logger.info("Blacklist loading time: " + blacklistTime + " ms");

            startTime = System.currentTimeMillis();
            GraphBuilder builder = new GraphBuilder(blacklist);
            builder.buildFromETN(etnFile);
            long etnBuildTime = System.currentTimeMillis() - startTime;
            Logger.info("ETN build time: " + etnBuildTime + " ms");

            Graph graph = builder.getGraph();
            graph.duplicateEdges();
            Logger.success("ETN graph: " + builder.getAddressMapper().size() + " nodes, and " + graph.edgeCount() + " edges");

            startTime = System.currentTimeMillis();
            NFTTraderLoader nftLoader = new NFTTraderLoader(blacklist, builder.getAddressMapper());
            Set<Integer> traders = nftLoader.loadTraders(nftFile);
            long nftLoadTime = System.currentTimeMillis() - startTime;
            Logger.info("NFT loading time: " + nftLoadTime + " ms");

            int nodeCount = builder.getAddressMapper().size();
            ParallelLinkabilityBuilder linkBuilder = new ParallelLinkabilityBuilder(graph, traders, maxDepth, nodeCount);
            linkBuilder.buildLinkabilityNetwork(outputFile);
            long totalTime = System.currentTimeMillis() - totalStartTime;
            Logger.success("===========================================");
            Logger.success("TOTAL RUNTIME: " + totalTime + " ms (" + (totalTime / 1000.0) + " seconds)");
            Logger.success("Results saved to: " + outputFile);
            Logger.success("==========================================");

        } catch (IOException | InterruptedException e) {
            Logger.error("Failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java Main <max_depth> <etn_file> <nft_file> <blacklist_folder> <output_file>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  max_depth         : Maximum traversal depth (1-10)");
        System.out.println("  etn_file          : Path to Ethereum Transaction Network CSV");
        System.out.println("  nft_file          : Path to NFT transfers CSV");
        System.out.println("  blacklist_folder  : Path to folder containing blacklist JSON files");
        System.out.println("  output_file       : Path to output linkability network CSV");
        System.out.println();
    }
}
