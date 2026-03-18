package com.citadel.ui;

import com.citadel.vault.VaultManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.nio.file.Files;

/**
 * Login screen — the first screen the user sees.
 *
 * <ul>
 *   <li>If the vault file does not exist, shows a "Create New Vault" flow.
 *   <li>If the vault file exists, shows an "Unlock Vault" flow.
 * </ul>
 */
public class LoginView {

    private final Stage stage;

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VaultManager vm     = AppContext.getVaultManager();
        boolean isNewVault  = !Files.exists(vm.getVaultPath());

        // ── Root layout ────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0d1117;");
        root.setPrefSize(480, 580);

        // ── Card ───────────────────────────────────────────────────────────────
        VBox card = new VBox(20);
        card.setMaxWidth(380);
        card.setPadding(new Insets(42, 48, 42, 48));
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
                -fx-background-color: #161b22;
                -fx-background-radius: 18;
                """);
        DropShadow shadow = new DropShadow(30, Color.BLACK);
        card.setEffect(shadow);

        // ── Icon / Logo ────────────────────────────────────────────────────────
        Text icon = new Text("🏰");
        icon.setFont(Font.font(52));

        // ── Title ──────────────────────────────────────────────────────────────
        Label title = new Label("Fortified Citadel");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#e6edf3"));

        Label subtitle = new Label(isNewVault ? "Create your secure vault" : "Enter your master password");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setTextFill(Color.web("#8b949e"));

        // ── Password field ─────────────────────────────────────────────────────
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Master password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        styleTextField(passwordField);

        // ── Confirm password (new vault only) ─────────────────────────────────
        PasswordField confirmField  = new PasswordField();
        confirmField.setPromptText("Confirm password");
        confirmField.setMaxWidth(Double.MAX_VALUE);
        styleTextField(confirmField);
        confirmField.setVisible(isNewVault);
        confirmField.setManaged(isNewVault);

        // ── Error label ────────────────────────────────────────────────────────
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#f85149"));
        errorLabel.setFont(Font.font("System", 12));
        errorLabel.setVisible(false);

        // ── Submit button ──────────────────────────────────────────────────────
        Button submitBtn = new Button(isNewVault ? "Create Vault" : "Unlock Vault");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        stylePrimaryButton(submitBtn);

        // ── Progress indicator ─────────────────────────────────────────────────
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        spinner.setVisible(false);

        // ── Action ────────────────────────────────────────────────────────────
        submitBtn.setOnAction(e -> {
            char[] password = passwordField.getText().toCharArray();
            if (password.length == 0) {
                showError(errorLabel, "Please enter your master password.");
                return;
            }
            if (isNewVault && !passwordField.getText().equals(confirmField.getText())) {
                showError(errorLabel, "Passwords do not match.");
                return;
            }
            if (isNewVault && password.length < 8) {
                showError(errorLabel, "Password must be at least 8 characters.");
                return;
            }

            submitBtn.setDisable(true);
            spinner.setVisible(true);

            // Offload Argon2 KDF to background thread; update UI on FX thread
            new Thread(() -> {
                try {
                    if (isNewVault) {
                        vm.initializeNewVault(password);
                    } else {
                        vm.unlock(password);
                    }
                    javafx.application.Platform.runLater(() -> openDashboard());
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        spinner.setVisible(false);
                        showError(errorLabel, isNewVault
                                ? "Failed to create vault: " + ex.getMessage()
                                : "Wrong password or corrupted vault.");
                    });
                }
            }, "kdf-thread").start();
        });

        // Allow Enter key to submit
        passwordField.setOnAction(e -> submitBtn.fire());
        confirmField.setOnAction(e -> submitBtn.fire());

        // ── Assemble card ─────────────────────────────────────────────────────
        card.getChildren().addAll(icon, title, subtitle,
                passwordField, confirmField,
                errorLabel, submitBtn, spinner);

        root.getChildren().add(card);

        // ── Scene ─────────────────────────────────────────────────────────────
        Scene scene = new Scene(root, 480, 580);
        stage.setScene(scene);
        stage.setTitle("Fortified Credential Citadel");
        stage.setResizable(false);
        stage.show();
    }

    private void openDashboard() {
        DashboardView dash = new DashboardView(stage);
        dash.show();
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("""
                -fx-background-color: #21262d;
                -fx-text-fill: #e6edf3;
                -fx-prompt-text-fill: #484f58;
                -fx-border-color: #30363d;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 10 14;
                -fx-font-size: 14;
                """);
    }

    private void stylePrimaryButton(Button btn) {
        btn.setStyle("""
                -fx-background-color: #238636;
                -fx-text-fill: white;
                -fx-background-radius: 8;
                -fx-font-size: 14;
                -fx-padding: 10 0;
                -fx-cursor: hand;
                """);
        btn.hoverProperty().addListener((obs, was, is) ->
                btn.setStyle(is ?
                        "-fx-background-color: #2ea043; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14; -fx-padding: 10 0; -fx-cursor:hand;"
                        : "-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14; -fx-padding: 10 0; -fx-cursor:hand;"));
    }
}
