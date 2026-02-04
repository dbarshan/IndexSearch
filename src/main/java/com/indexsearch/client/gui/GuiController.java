package com.indexsearch.client.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indexsearch.client.model.CollectionInfo;
import com.indexsearch.client.service.ClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.InlineCssTextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class GuiController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COLOR_KEY = "#000000";
    private static final String COLOR_STRING = "#556b2f";
    private static final String COLOR_NUMBER = "#d16969";
    private static final String COLOR_BOOLEAN = "#d7ba7d";
    private static final String COLOR_NULL = "#808080";
    private static final String COLOR_PUNCT = "#d4d4d4";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ClientService clientService;
    private final ContextMenu resultMenu = new ContextMenu();
    private final Image collectionIcon = loadIcon("/client/gui/collection.png");

    @Value("${indexsearch.server.url:http://localhost:8080}")
    private String serverBaseUrl;

    @FXML
    private ListView<CollectionInfo> listConnection;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblConnection;

    @FXML
    private TextArea txtQueryBox;

    @FXML
    private InlineCssTextArea txtResultBox;

    @FXML
    public void initialize() {
        listConnection.setItems(FXCollections.observableArrayList());
        listConnection.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CollectionInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (collectionIcon != null) {
                    ImageView iconView = new ImageView(collectionIcon);
                    iconView.setFitWidth(14);
                    iconView.setFitHeight(14);
                    iconView.setPreserveRatio(true);
                    setGraphic(iconView);
                } else {
                    setGraphic(null);
                }
                setText(item.getName());
            }
        });
        setupResultMenu();
    }

    public GuiController(ClientService clientService) {
        this.clientService = clientService;
    }

    @FXML
    public void onRefreshCollections() {
        refreshCollections();
    }

    @FXML
    public void onRunQuery() {
        String query = txtQueryBox.getText();
        if (query == null || query.isBlank()) {
            setStatus("Query is empty.");
            return;
        }
        String selectedCollection = null;
        CollectionInfo selected = listConnection.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedCollection = selected.getName();
        }
        runQuery(query.trim(), selectedCollection);
    }

    private void refreshCollections() {
        String resolvedUrl = serverBaseUrl == null || serverBaseUrl.isBlank()
                ? "http://localhost:8080"
                : serverBaseUrl.trim();
        setStatus("Connecting ...");
        setConnection(resolvedUrl);
        executor.submit(() -> {
            try {
                List<CollectionInfo> collections = clientService.listCollections();
                Platform.runLater(() -> {
                    listConnection.setItems(FXCollections.observableArrayList(collections));
                    if (!collections.isEmpty()) {
                        listConnection.getSelectionModel().select(0);
                    }
                    setStatus("Collections: " + collections.size());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setConnection("disconnected");
                    setStatus("Failed to load collections: " + e.getMessage());
                });
            }
        });
    }

    private void runQuery(String query, String collection) {
        if (clientService == null) {
            setResult("Client service not ready.");
            return;
        }
        executor.submit(() -> {
            try {
                List<JsonNode> results = query.toLowerCase().startsWith("select")
                        ? clientService.sql(query)
                        : runSimpleQuery(collection, query);
                String pretty = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(results);
                Platform.runLater(() -> {
                    setResult(pretty);
                    setStatus("Results: " + results.size());
                });
            } catch (Exception e) {
                Platform.runLater(() -> setResult("Error: " + e.getMessage()));
            }
        });
    }

    private List<JsonNode> runSimpleQuery(String collection, String query) {
        if (collection == null || collection.isBlank()) {
            throw new IllegalArgumentException("Select a collection for a simple query.");
        }
        return clientService.search(collection, query);
    }

    private void setStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    private void setConnection(String connectionName) {
        if (lblConnection != null) {
            lblConnection.setText(connectionName);
        }
    }

    private void setResult(String message) {
        if (txtResultBox == null) {
            return;
        }
        txtResultBox.replaceText("");
        if (message == null || message.isBlank()) {
            return;
        }
        String trimmed = message.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            appendStyled(message, COLOR_PUNCT);
            return;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(message);
            appendJson(node, 0);
        } catch (Exception e) {
            appendStyled(message, COLOR_PUNCT);
        }
    }

    private void appendJson(JsonNode node, int indent) {
        if (node == null || node.isNull()) {
            appendStyled("null", COLOR_NULL);
            return;
        }
        if (node.isObject()) {
            appendStyled("{\n", COLOR_PUNCT);
            int size = node.size();
            int index = 0;
            for (var it = node.fields(); it.hasNext(); ) {
                var entry = it.next();
                appendIndent(indent + 2);
                appendStyled("\"" + entry.getKey() + "\"", COLOR_KEY);
                appendStyled(": ", COLOR_PUNCT);
                appendJson(entry.getValue(), indent + 2);
                if (index < size - 1) {
                    appendStyled(",", COLOR_PUNCT);
                }
                appendStyled("\n", COLOR_PUNCT);
                index++;
            }
            appendIndent(indent);
            appendStyled("}", COLOR_PUNCT);
            return;
        }
        if (node.isArray()) {
            appendStyled("[\n", COLOR_PUNCT);
            int size = node.size();
            for (int i = 0; i < size; i++) {
                appendIndent(indent + 2);
                appendJson(node.get(i), indent + 2);
                if (i < size - 1) {
                    appendStyled(",", COLOR_PUNCT);
                }
                appendStyled("\n", COLOR_PUNCT);
            }
            appendIndent(indent);
            appendStyled("]", COLOR_PUNCT);
            return;
        }
        if (node.isTextual()) {
            appendStyled("\"" + node.asText() + "\"", COLOR_STRING);
        } else if (node.isNumber()) {
            appendStyled(node.numberValue().toString(), COLOR_NUMBER);
        } else if (node.isBoolean()) {
            appendStyled(Boolean.toString(node.asBoolean()), COLOR_BOOLEAN);
        } else {
            appendStyled(node.asText(), COLOR_STRING);
        }
    }

    private void appendIndent(int spaces) {
        if (spaces <= 0) {
            return;
        }
        appendStyled(" ".repeat(spaces), COLOR_PUNCT);
    }

    private void appendStyled(String value, String color) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int start = txtResultBox.getLength();
        txtResultBox.appendText(value);
        txtResultBox.setStyle(start, start + value.length(), "-fx-fill: " + color + ";");
    }

    private Image loadIcon(String path) {
        try {
            return new Image(GuiController.class.getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    private void setupResultMenu() {
        if (txtResultBox == null) {
            return;
        }
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> txtResultBox.copy());
        resultMenu.getItems().setAll(copy);
        txtResultBox.setContextMenu(resultMenu);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
