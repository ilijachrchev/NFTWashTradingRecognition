package utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String RESET = "\u001B[0m";

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private static String buildPrefix(String level, String color) {
        String time = timeFormat.format(new Date());
        String thread = Thread.currentThread().getName();
        return "[" + time + "]" + color + level + RESET + ": ";
    }

    public static void info(String message) {
        System.out.println(buildPrefix("INFO", BLUE) + message);
    }
    public static void warn(String message) {
        System.out.println(buildPrefix("WARN", BLUE) + message);
    }
    public static void error(String message) {
        System.out.println(buildPrefix("ERROR", RED) + message);
    }
    public static void debug(String message) {
        System.out.println(buildPrefix("DEBUG", YELLOW) + message);
    }
    public static void success(String message) {
        System.out.println(buildPrefix("SUCCESS", GREEN) + message);
    }
}
