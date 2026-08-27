package org.example.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GameLogger {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";
    private static final String BLUE = "\u001B[34m";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static String getTimestamp() {
        return "[" + LocalTime.now().format(TIME_FORMAT) + "]";
    }

    public static void banner() {
        System.out.println(CYAN + """
            ╔════════════════════════════════════════════════════════╗
            ║            CYBER CASTLE - 3D TANK BATTLE               ║
            ║              Engine: jME 3.7.0 | LWJGL 3               ║
            ╚════════════════════════════════════════════════════════╝
            """ + RESET);
    }

    public static void info(String module, String message) {
        System.out.printf("%s %s[INFO]%s %s[%-12s]%s %s%n",
                getTimestamp(), GREEN, RESET, CYAN, module, RESET, message);
    }

    public static void combat(String action, String details) {
        System.out.printf("%s %s[COMBAT]%s %s%-12s%s -> %s%n",
                getTimestamp(), PURPLE, RESET, YELLOW, action, RESET, details);
    }

    public static void warn(String module, String message) {
        System.out.printf("%s %s[WARN]%s %s[%-12s]%s %s%n",
                getTimestamp(), YELLOW, RESET, YELLOW, module, RESET, message);
    }

    public static void error(String module, String message) {
        System.err.printf("%s %s[ERROR]%s %s[%-12s]%s %s%n",
                getTimestamp(), RED, RESET, RED, module, RESET, message);
    }

    public static void network(String event, String payload) {
        System.out.printf("%s %s[NET]%s %s[%-12s]%s %s%n",
                getTimestamp(), BLUE, RESET, BLUE, event, RESET, payload);
    }
}