package com.citadel.ui;

import com.citadel.model.ApiTokenCredential;
import com.citadel.model.PasswordCredential;
import com.citadel.model.SecureNoteCredential;
import com.citadel.model.VaultItem;
import com.citadel.vault.VaultManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

/**
 * Main dashboard view — shown after a successful vault unlock.
 *
 * <p>Shows all vault items in a searchable list on the left.
 * Selecting an item shows its details on the right.
 * Toolbar buttons let the user add, delete, lock, and search.
 */
public class DashboardView {

    private final Stage stage;
    private final VaultManager vm;

    private final ObservableList<VaultItem> displayedItems = FXCollections.observableArrayList();
    private ListView<VaultItem>             listView;
    private VBox                            detailPane;
    private TextField                       searchBox;

    public DashboardView(Stage stage) {
        this.stage = stage;
        this.vm    = AppContext.getVaultManager();
    }

    public void show() {

        // ── Top bar ────────────────────────────────────────────────────────
        HBox topBar = buildTopBar();

        // ── Left — search + list ───────────────────────────────────────────
        VBox leftPane = buildLeftPane();

        // ── Right — item detail ────────────────────────────────────────────
        detailPane = buildEmptyDetailPane();

        // ── Split layout ───────────────────────────────────────────────────
        HBox content = new HBox(0, leftPane, detailPane);
        HBox.setHgrow(detailPane, Priority.ALWAYS);

        VBox root = new VBox(0, topBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #0d1117;");

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Fortified Credential Citadel — Vault");
        stage.setResizable(true);
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.show();

        // populate list
        refreshList();
    }

    // -------------------------------------------------------------------------
    // Top Bar
    // -------------------------------------------------------------------------

    private HBox buildTopBar() {
        Label appTitle = new Label("🏰  Fortified Citadel");
        appTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        appTitle.setTextFill(Color.web("#e6edf3"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button lockBtn = iconButton("🔒  Lock", "#da3633");
        lockBtn.setOnAction(e -> {
            vm.lock();
            LoginView lv = new LoginView(stage);
            lv.show();
        });

        HBox bar = new HBox(16, appTitle, spacer, lockBtn);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    // -------------------------------------------------------------------------
    // Left Pane — search + item list
    // -------------------------------------------------------------------------

    private VBox buildLeftPane() {
        searchBox = new TextField();
        searchBox.setPromptText("🔍  Search…");
        searchBox.setStyle("""
                -fx-background-color: #21262d;
                -fx-text-fill: #e6edf3;
                -fx-prompt-text-fill: #484f58;
                -fx-border-color: #30363d;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12;
                -fx-font-size: 13;
                """);
        searchBox.textProperty().addListener((obs, old, q) -> filterList(q));

        listView = new ListView<>(displayedItems);
        listView.setStyle("-fx-background-color: #0d1117; -fx-border-color: transparent;");
        listView.setCellFactory(lv -> new VaultItemCell());
        listView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, item) -> showDetail(item));
        VBox.setVgrow(listView, Priority.ALWAYS);

        Button addBtn = new Button("＋  Add Credential");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle("""
                -fx-background-color: #238636;
                -fx-text-fill: white;
                -fx-background-radius: 8;
                -fx-font-size: 13;
                -fx-padding: 8 0;
                -fx-cursor: hand;
                """);
        addBtn.setOnAction(e -> openAddDialog());

        VBox left = new VBox(10, searchBox, listView, addBtn);
        left.setPadding(new Insets(14, 12, 14, 14));
        left.setPrefWidth(270);
        left.setMinWidth(220);
        left.setStyle("-fx-background-color: #0d1117; -fx-border-color: #21262d; -fx-border-width: 0 1 0 0;");
        return left;
    }

    // -------------------------------------------------------------------------
    // Detail Pane
    // -------------------------------------------------------------------------

    private VBox buildEmptyDetailPane() {
        Label placeholder = new Label("Select a credential to view its details");
        placeholder.setTextFill(Color.web("#484f58"));
        placeholder.setFont(Font.font("System", 14));
        VBox pane = new VBox(placeholder);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: #0d1117;");
        return pane;
    }

    private void showDetail(VaultItem item) {
        detailPane.getChildren().clear();
        if (item == null) {
            detailPane.setAlignment(Pos.CENTER);
            Label lbl = new Label("Select a credential to view its details");
            lbl.setTextFill(Color.web("#484f58"));
            detailPane.getChildren().add(lbl);
            return;
        }

        detailPane.setAlignment(Pos.TOP_LEFT);
        detailPane.setPadding(new Insets(28, 32, 28, 32));

        String typeEmoji = switch (item.getType()) {
            case PASSWORD    -> "🔑";
            case API_TOKEN   -> "🪙";
            case SECURE_NOTE -> "📝";
        };

        Label typeIcon = new Label(typeEmoji + "  " + item.getLabel());
        typeIcon.setFont(Font.font("System", FontWeight.BOLD, 22));
        typeIcon.setTextFill(Color.web("#e6edf3"));

        Label typeBadge = new Label(item.getType().name().replace("_", " "));
        typeBadge.setFont(Font.font("System", 11));
        typeBadge.setStyle("-fx-background-color: #21262d; -fx-text-fill: #8b949e; -fx-padding: 2 8; -fx-background-radius:6;");

        VBox fields = new VBox(14);
        fields.setPadding(new Insets(20, 0, 0, 0));

        if (item instanceof PasswordCredential p) {
            fields.getChildren().addAll(
                    fieldRow("Username", p.getUsername(), false),
                    fieldRow("Password", p.getPassword(), true),
                    fieldRow("URL",      p.getUrl(),      false),
                    fieldRow("Notes",    p.getNotes(),    false)
            );
        } else if (item instanceof ApiTokenCredential t) {
            fields.getChildren().addAll(
                    fieldRow("Service", t.getServiceName(), false),
                    fieldRow("Token",   t.getToken(),       true),
                    fieldRow("Expires", t.getExpiresAt() != null ? t.getExpiresAt().toString() : "Never", false),
                    fieldRow("Notes",   t.getNotes(),       false)
            );
        } else if (item instanceof SecureNoteCredential n) {
            fields.getChildren().addAll(
                    fieldRow("Content", n.getContent(), false)
            );
        }

        // ── Delete button ──────────────────────────────────────────────────
        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.setStyle("-fx-background-color: #da3633; -fx-text-fill: white; -fx-background-radius:8; -fx-padding: 7 18; -fx-cursor:hand;");
        deleteBtn.setOnAction(e -> confirmDelete(item));

        detailPane.getChildren().addAll(typeIcon, typeBadge, fields, sp, deleteBtn);
    }

    // -------------------------------------------------------------------------
    // Add Credential Dialog
    // -------------------------------------------------------------------------

    private void openAddDialog() {
        AddCredentialDialog dialog = new AddCredentialDialog(stage);
        dialog.showAndWait().ifPresent(item -> {
            try {
                vm.addItem(item);
                vm.save();
                refreshList();
            } catch (Exception ex) {
                showAlert("Error", "Failed to save credential: " + ex.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    private void confirmDelete(VaultItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + item.getLabel() + "\"? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        styleAlert(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    vm.removeItem(item.getId());
                    vm.save();
                    refreshList();
                    showDetail(null);
                } catch (Exception ex) {
                    showAlert("Error", "Failed to delete: " + ex.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void refreshList() {
        List<VaultItem> all = vm.getAllItems();
        displayedItems.setAll(all);
    }

    private void filterList(String query) {
        if (query == null || query.isBlank()) {
            displayedItems.setAll(vm.getAllItems());
        } else {
            String lower = query.toLowerCase();
            displayedItems.setAll(
                    vm.getAllItems().stream()
                      .filter(i -> i.getLabel().toLowerCase().contains(lower))
                      .toList()
            );
        }
    }

    private HBox fieldRow(String label, String value, boolean masked) {
        Label key = new Label(label + ":");
        key.setMinWidth(90);
        key.setFont(Font.font("System", FontWeight.BOLD, 13));
        key.setTextFill(Color.web("#8b949e"));

        if (value == null || value.isBlank()) value = "—";

        if (masked) {
            // Show masked by default with a toggle
            final String finalValue = value;
            Label val = new Label("••••••••••••");
            val.setFont(Font.font("System", 13));
            val.setTextFill(Color.web("#e6edf3"));
            Button toggle = new Button("👁");
            toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-cursor:hand; -fx-padding:0;");
            final boolean[] shown = {false};
            toggle.setOnAction(e -> {
                shown[0] = !shown[0];
                val.setText(shown[0] ? finalValue : "••••••••••••");
            });

            // Copy button
            Button copy = new Button("📋");
            copy.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-cursor:hand; -fx-padding:0;");
            copy.setOnAction(e -> {
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                        new javafx.scene.input.ClipboardContent() {{ putString(finalValue); }});
            });

            return new HBox(10, key, val, toggle, copy);
        } else {
            Label val = new Label(value);
            val.setFont(Font.font("System", 13));
            val.setTextFill(Color.web("#e6edf3"));
            val.setWrapText(true);

            // Copy button
            final String finalValue = value;
            Button copy = new Button("📋");
            copy.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-cursor:hand; -fx-padding:0;");
            copy.setOnAction(e -> {
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                        new javafx.scene.input.ClipboardContent() {{ putString(finalValue); }});
            });

            return new HBox(10, key, val, copy);
        }
    }

    private Button iconButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor:hand;");
        return btn;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle("-fx-background-color:#161b22; -fx-text-fill:#e6edf3;");
    }
}
