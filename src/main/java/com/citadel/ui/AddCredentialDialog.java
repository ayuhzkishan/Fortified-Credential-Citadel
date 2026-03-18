package com.citadel.ui;

import com.citadel.factory.CredentialFactory;
import com.citadel.model.VaultItem;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.Optional;

/**
 * Dialog for adding a new credential to the vault.
 *
 * <p>Lets the user select the credential type (Password, API Token, Secure Note)
 * and fill in the relevant fields, then returns the new {@link VaultItem}.
 */
public class AddCredentialDialog {

    private final Stage owner;

    public AddCredentialDialog(Stage owner) {
        this.owner = owner;
    }

    public Optional<VaultItem> showAndWait() {
        Dialog<VaultItem> dialog = new Dialog<>();
        dialog.setTitle("Add New Credential");
        dialog.setHeaderText(null);
        dialog.initOwner(owner);

        // ── Dark theme for the dialog ─────────────────────────────────────
        dialog.getDialogPane().setStyle("""
                -fx-background-color: #161b22;
                -fx-text-fill: #e6edf3;
                """);

        // ── Type selector at the top ─────────────────────────────────────
        ToggleGroup typeGroup   = new ToggleGroup();
        ToggleButton passBtn    = toggleBtn("🔑 Password",    typeGroup);
        ToggleButton tokenBtn   = toggleBtn("🪙 API Token",   typeGroup);
        ToggleButton noteBtn    = toggleBtn("📝 Secure Note", typeGroup);
        passBtn.setSelected(true);
        HBox typeBar = new HBox(6, passBtn, tokenBtn, noteBtn);

        // ── Common fields ─────────────────────────────────────────────────
        TextField labelField    = styledField("Label / Name");

        // Password-specific
        TextField usernameField = styledField("Username / Email");
        PasswordField passField  = new PasswordField();
        passField.setPromptText("Password");
        styleField(passField);
        TextField urlField      = styledField("URL (optional)");
        TextField notesField    = styledField("Notes (optional)");

        // API Token-specific
        TextField serviceField  = styledField("Service name");
        TextField tokenField    = styledField("Token / Secret");
        TextField expiresField  = styledField("Expires at (ISO-8601, optional)");
        TextField tokenNotesField = styledField("Notes (optional)");

        // Secure Note-specific
        TextArea contentArea    = new TextArea();
        contentArea.setPromptText("Note content…");
        contentArea.setPrefRowCount(4);
        contentArea.setStyle("""
                -fx-background-color: #21262d;
                -fx-text-fill: #e6edf3;
                -fx-prompt-text-fill: #484f58;
                -fx-border-color: #30363d;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-control-inner-background: #21262d;
                -fx-font-size: 13;
                """);

        // ── Dynamic form region ───────────────────────────────────────────
        VBox passwordForm   = new VBox(10, row("Username:",  usernameField),
                                            row("Password:",  passField),
                                            row("URL:",       urlField),
                                            row("Notes:",     notesField));
        VBox tokenForm      = new VBox(10, row("Service:",   serviceField),
                                            row("Token:",    tokenField),
                                            row("Expires:",  expiresField),
                                            row("Notes:",    tokenNotesField));
        VBox noteForm       = new VBox(10, row("Content:",   contentArea));

        StackPane formStack = new StackPane(passwordForm, tokenForm, noteForm);
        tokenForm.setVisible(false);
        noteForm.setVisible(false);

        typeGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
            passwordForm.setVisible(selected == passBtn);
            tokenForm.setVisible(selected == tokenBtn);
            noteForm.setVisible(selected == noteBtn);
        });

        // ── Error label ───────────────────────────────────────────────────
        Label errorLbl = new Label();
        errorLbl.setTextFill(Color.web("#f85149"));
        errorLbl.setFont(Font.font("System", 12));

        // ── Main layout ───────────────────────────────────────────────────
        VBox layout = new VBox(14,
                sectionLabel("Credential Type"), typeBar,
                sectionLabel("Label"),           labelField,
                sectionLabel("Details"),         formStack,
                errorLbl);
        layout.setPadding(new Insets(24, 28, 10, 28));
        layout.setPrefWidth(440);
        dialog.getDialogPane().setContent(layout);

        // ── Buttons ───────────────────────────────────────────────────────
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // ── Style the Save button ─────────────────────────────────────────
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius:8;");

        // ── Result converter ──────────────────────────────────────────────
        dialog.setResultConverter(btn -> {
            if (btn != saveType) return null;

            String label = labelField.getText().trim();
            if (label.isBlank()) {
                errorLbl.setText("Label is required.");
                return null;
            }

            try {
                Toggle sel = typeGroup.getSelectedToggle();
                if (sel == passBtn) {
                    return CredentialFactory.createPassword(
                            label,
                            usernameField.getText().trim(),
                            passField.getText(),
                            urlField.getText().trim(),
                            notesField.getText().trim()
                    );
                } else if (sel == tokenBtn) {
                    Instant exp = null;
                    if (!expiresField.getText().isBlank()) {
                        exp = Instant.parse(expiresField.getText().trim());
                    }
                    return CredentialFactory.createApiToken(
                            label,
                            serviceField.getText().trim(),
                            tokenField.getText().trim(),
                            tokenNotesField.getText().trim(),
                            exp
                    );
                } else {
                    return CredentialFactory.createSecureNote(
                            label,
                            contentArea.getText().trim()
                    );
                }
            } catch (Exception e) {
                errorLbl.setText("Invalid input: " + e.getMessage());
                return null;
            }
        });

        return dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HBox row(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setMinWidth(80);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#8b949e"));
        HBox.setHgrow(field instanceof TextField || field instanceof PasswordField || field instanceof TextArea
                ? (Region) field : new Region(), Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setMaxWidth(Double.MAX_VALUE);
        return new HBox(10, lbl, field);
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        styleField(tf);
        return tf;
    }

    private void styleField(TextField tf) {
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("""
                -fx-background-color: #21262d;
                -fx-text-fill: #e6edf3;
                -fx-prompt-text-fill: #484f58;
                -fx-border-color: #30363d;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-padding: 7 10;
                -fx-font-size: 13;
                """);
    }

    private ToggleButton toggleBtn(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setStyle("""
                -fx-background-color: #21262d;
                -fx-text-fill: #8b949e;
                -fx-background-radius: 8;
                -fx-padding: 6 14;
                -fx-cursor: hand;
                """);
        btn.selectedProperty().addListener((obs, was, is) ->
            btn.setStyle(is
                ? "-fx-background-color: #238636; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor:hand;"
                : "-fx-background-color: #21262d; -fx-text-fill: #8b949e; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor:hand;"));
        return btn;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#484f58"));
        return lbl;
    }
}
