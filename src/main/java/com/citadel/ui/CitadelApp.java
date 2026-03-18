package com.citadel.ui;

import com.citadel.crypto.AesGcmService;
import com.citadel.crypto.Argon2DerivationService;
import com.citadel.vault.VaultManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JavaFX entry point for Fortified Credential Citadel.
 *
 * <p>Creates the shared {@link VaultManager} and opens the Login screen.
 * All UI controllers receive the same manager instance via the {@link AppContext}.
 *
 * @author Ayush Kishan
 */
public class CitadelApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Wire up the core engine
        Path vaultPath    = Paths.get(System.getProperty("user.home"), ".citadel", "vault.citadel");
        AesGcmService aes = new AesGcmService();
        Argon2DerivationService kdf = new Argon2DerivationService();
        VaultManager vaultManager   = new VaultManager(vaultPath, aes, kdf);

        // Store in application-wide context
        AppContext.init(vaultManager);

        // Show login window
        LoginView loginView = new LoginView(primaryStage);
        loginView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
