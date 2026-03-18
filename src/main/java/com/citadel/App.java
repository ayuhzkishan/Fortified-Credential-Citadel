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
        
        try {
            // Setup paths and dependencies
            java.nio.file.Path vaultPath = java.nio.file.Paths.get("my_vault.citadel");
            com.citadel.crypto.AesGcmService aesService = new com.citadel.crypto.AesGcmService();
            com.citadel.crypto.Argon2DerivationService kdfService = new com.citadel.crypto.Argon2DerivationService();
            
            com.citadel.vault.VaultManager manager = new com.citadel.vault.VaultManager(vaultPath, aesService, kdfService);
            char[] masterPassword = "MyStrongPassword123!".toCharArray();

            if (!java.nio.file.Files.exists(vaultPath)) {
                logger.info("Creating a new Vault!");
                manager.initializeNewVault(masterPassword);
                
                // Add a sample credential
                com.citadel.model.PasswordCredential github = com.citadel.factory.CredentialFactory.createPassword(
                        "GitHub Account", "dev_ayush", "secret_pass_99", "https://github.com", "My personal github");
                
                manager.addItem(github);
                manager.save();
                manager.lock();
                logger.info("Vault initialized, saved, and locked successfully.");
            }

            logger.info("Unlocking Vault...");
            manager.unlock(masterPassword);
            
            logger.info("Retrieved {} credentials from the vault.", manager.getAllItems().size());
            for (com.citadel.model.VaultItem item : manager.getAllItems()) {
                if (item instanceof com.citadel.model.PasswordCredential p) {
                    logger.info("Found: {} (Username: {}, Password: {})", p.getLabel(), p.getUsername(), p.getPassword());
                }
            }

            manager.lock();
            logger.info("Vault locked. Memory wiped.");
            
        } catch (Exception e) {
            logger.error("Demo failed!", e);
        }
    }
}
