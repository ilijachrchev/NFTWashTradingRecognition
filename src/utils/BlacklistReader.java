package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class BlacklistReader {

    public static Set<String> loadBlacklist(String folderPath) throws IOException {
        Set<String> blacklist = new HashSet<>();

        File folder = new File(folderPath);

        if(!folder.exists() || !folder.isDirectory()) {
            Logger.warn("Blacklist folder not found for: " + folderPath);
            Logger.warn("Continuing without filtering the blacklist!");
            return blacklist;
        }

        // getting all json files
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            Logger.warn("No files found in the blacklist folder: " + folderPath);
            return blacklist;
        }

        Logger.info("Loading blacklist!");

        for (File file : files) {
            int beforeSize = blacklist.size();
            loadFromFile(file.toPath(), blacklist);
            int added = blacklist.size() - beforeSize;
            Logger.debug(" " + file.getName() + ": " + added + " addresses!");
        }
        Logger.success("Loaded " + blacklist.size() + " blacklist addresses!");
        return blacklist;
    }

    private static void loadFromFile(Path filepath, Set<String> blacklist) throws IOException {
        String content = Files.readString(filepath);

        String[] parts = content.split("\"");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("0x")) {
                blacklist.add(trimmed.toLowerCase());
            }
        }

    }
}
