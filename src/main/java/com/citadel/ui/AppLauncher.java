package com.citadel.ui;

/**
 * A wrapper class to launch the JavaFX application.
 * 
 * <p>Why do we need this? 
 * Java 11+ removed JavaFX from the core JDK. If you try to run a class that extends 
 * {@code javafx.application.Application} directly from your IDE, it expects the JavaFX 
 * modules to be on the module path.
 * 
 * By launching from this class (which doesn't extend Application), the IDE will load 
 * the JavaFX dependencies from the standard classpath automatically.
 */
public class AppLauncher {
    public static void main(String[] args) {
        CitadelApp.main(args);
    }
}
