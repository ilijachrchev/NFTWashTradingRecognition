package builder;

import model.AddressMapper;
import model.Graph;
import utils.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class GraphBuilder {

    private final AddressMapper addressMapper;
    private final Graph graph;
    private final Set<String> blacklist;

    public GraphBuilder(Set<String> blacklist) {
        this.addressMapper = new AddressMapper();
        this.graph = new Graph();
        this.blacklist = blacklist;
    }

    public void buildFromETN(String etnFilePath) throws IOException {
        Logger.info("Builidng ETN graph from: " + etnFilePath);

        long skipped = 0;
        long added = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(etnFilePath))) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String from = parts[0].trim();
                String to = parts[1].trim();

                if (isBlackListed(from) || isBlackListed(to)) {
                    skipped++;
                    continue;
                }

                int fromId = addressMapper.getOrCreateId(from);
                int toId = addressMapper.getOrCreateId(to);

                graph.addEdge(fromId, toId);
                added++;

                if (added % 1_000_000 == 0) {
                    Logger.info("Added " + added + " edges, skipped " + skipped);
                }
            }
        }
    }

    public boolean isBlackListed(String address) {
        if (address == null || address.isEmpty()) return true;
        return blacklist.contains(address.toLowerCase());
    }

    public AddressMapper getAddressMapper() {
        return addressMapper;
    }
    public Graph getGraph() {
        return graph;
    }
}
