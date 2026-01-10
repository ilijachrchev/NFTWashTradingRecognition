import builder.GraphBuilder;
import model.Graph;
import utils.BlacklistReader;
import utils.Logger;

import java.io.IOException;
import java.util.Set;

public class Example{

    public static void main(String[] args) {
        try {
            Logger.info("Starting ETN build process");

            String blacklistFolder = "D:\\nft\\untitled\\blacklist";
            Set<String> blacklist = BlacklistReader.loadBlacklist(blacklistFolder);

            GraphBuilder builder = new GraphBuilder(blacklist);

            String etnFile = "D:\\nft\\untitled\\prog3ETNsample.csv";
            builder.buildFromETN(etnFile);

            Graph graph = builder.getGraph();

            Logger.success("Final graph: " + graph.nodeCount() + " nodes, " + graph.edgeCount() + " edges");
        } catch (IOException e) {
            Logger.error("Failed to build ETN: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
