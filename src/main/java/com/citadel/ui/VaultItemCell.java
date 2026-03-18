package com.citadel.ui;

import com.citadel.model.VaultItem;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Custom ListCell that renders a {@link VaultItem} as a styled card row.
 */
public class VaultItemCell extends ListCell<VaultItem> {

    private final VBox   container;
    private final Label  labelText;
    private final Label  typeText;

    public VaultItemCell() {
        container = new VBox(2);
        container.setPadding(new Insets(10, 14, 10, 14));

        labelText = new Label();
        labelText.setFont(Font.font("System", FontWeight.BOLD, 13));
        labelText.setTextFill(Color.web("#e6edf3"));

        typeText = new Label();
        typeText.setFont(Font.font("System", 11));
        typeText.setTextFill(Color.web("#484f58"));

        container.getChildren().addAll(labelText, typeText);
        setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void updateItem(VaultItem item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        String emoji = switch (item.getType()) {
            case PASSWORD    -> "🔑";
            case API_TOKEN   -> "🪙";
            case SECURE_NOTE -> "📝";
        };

        labelText.setText(emoji + "  " + item.getLabel());
        typeText.setText(item.getType().name().replace("_", " "));

        if (isSelected()) {
            container.setStyle("-fx-background-color: #21262d; -fx-background-radius: 10;");
        } else {
            container.setStyle("-fx-background-color: transparent;");
        }

        setGraphic(container);
        setText(null);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 4;");

        // Update selection styling dynamically
        selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) {
                container.setStyle("-fx-background-color: #21262d; -fx-background-radius: 10;");
            } else {
                container.setStyle("-fx-background-color: transparent;");
            }
        });
    }
}
