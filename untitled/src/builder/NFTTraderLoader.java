package builder;

import model.AddressMapper;
import utils.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NFTTraderLoader {

    private static final String NULL_ADDRESS = "0x0000000000000000000000000000000000000000";
    private final Set<String> blacklist;
    private final AddressMapper addressMapper;

    public NFTTraderLoader(final Set<String> blacklist, final AddressMapper addressMapper) {
        this.blacklist = blacklist;
        this.addressMapper = addressMapper;
    }

    public Set<Integer> loadTraders(String nftFilePath) throws IOException {
        Logger.info("Loading NFT traders from: " + nftFilePath);

        Set<Integer> traderIds = new HashSet<>();
        long totalRecords = 0;
        long skippedNull = 0;
        long skippedBlacklist = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(nftFilePath))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length < 6) continue;

                String from = parts[4].trim();
                String to = parts[5].trim();

                totalRecords++;

                if (!to.equalsIgnoreCase(NULL_ADDRESS)) {
                    if (!isBlacklisted(from)) {
                        int fromId = addressMapper.getOrCreateId(from);
                        traderIds.add(fromId);
                    } else {
                        skippedBlacklist++;
                    }
                } else {
                    skippedNull++;
                }

                if (!to.equalsIgnoreCase(NULL_ADDRESS)) {
                    if (!isBlacklisted(to)) {
                        int toId = addressMapper.getOrCreateId(to);
                        traderIds.add(toId);
                    } else {
                        skippedBlacklist++;
                    }
                } else {
                    skippedNull++;
                }
            }
        }
        Logger.success("Loaded " + traderIds.size() + " unique NFT traders!");
        Logger.info("Skipped blacklist addresses: " + skippedBlacklist);

        return traderIds;
    }
    private boolean isBlacklisted(String address) {
        if (address == null || address.isEmpty()) return true;
        return blacklist.contains(address.toLowerCase());
    }
}
