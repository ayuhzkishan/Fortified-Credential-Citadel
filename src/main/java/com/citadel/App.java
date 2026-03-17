package com.citadel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Fortified Credential Citadel application.
 *
 * Responsibilities:
 *  - Bootstrap the application
 *  - Will be wired to the Desktop UI in a later phase
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("======================================");
        logger.info("  Fortified Credential Citadel v1.0  ");
        logger.info("======================================");
        logger.info("Core engine starting up...");
        // Future: launch JavaFX Desktop UI here
    }
}
