import builder.GraphBuilder;
import builder.NFTTraderLoader;
import com.sun.source.doctree.SeeTree;
import model.Graph;
import mpi.MPI;
import utils.BlacklistReader;
import utils.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class DistributedMain {

    private static final int MAX_DEPTH = 3;
    private static final String ETN_FILE = "untitled/prog3ETNsample.csv";
    private static final String NFT_FILE = "untitled/boredapeyachtclub.csv";
    private static final String BLACKLIST_FOLDER = "untitled/blacklist";
    private static final String OUTPUT_FILE = "untitled/output.csv";

    public static void main(String[] args) throws IOException {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        String[] userArgs = Arrays.copyOfRange(args, 3, args.length);

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
            Logger.info("==============================================================================");
        }

        Set<String> blacklist = BlacklistReader.loadBlacklist(blacklistFolder);

        GraphBuilder builder = new GraphBuilder(blacklist);
        builder.buildFromETN(etnFile);
        Graph graph = builder.getGraph();
        graph.duplicateEdges();

        NFTTraderLoader nftLoader = new NFTTraderLoader(blacklist, builder.getAddressMapper());
        Set<Integer> traderSet = nftLoader.loadTraders(nftFile);
        int[] traders = traderSet.stream().mapToInt(Integer::intValue).sorted().toArray();

        System.out.println("[rank " + rank + "] nodes= " + builder.getAddressMapper().size() + " edges= " + graph.edgeCount() + " traders= " + traders.length);

        MPI.COMM_WORLD.Barrier();

        if (rank == 0) {
            Logger.success("All ranks loaded daya successfully");
        }

        MPI.Finalize();
    }
}
