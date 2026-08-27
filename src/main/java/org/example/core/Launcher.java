package org.example.core;


import org.example.util.GameLogger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {

    public static void main(String[] args) {

        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        Logger.getLogger("org.lwjgl").setLevel(Level.WARNING);

        GameLogger.banner();
        GameLogger.info("Bootstrap", "Starting Cyber Castle client on thread: " + Thread.currentThread().getName());

        Main3DApp.main(args);
    }
}