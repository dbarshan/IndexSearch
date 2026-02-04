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
import javafx.scene.text.TextFlow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class GuiController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ClientService clientService;

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
    private TextFlow txtResultBox;

    @FXML
    public void initialize() {
        listConnection.setItems(FXCollections.observableArrayList());
        listConnection.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CollectionInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getName());
            }
        });
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
        txtResultBox.getChildren().setAll(new javafx.scene.text.Text(message));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
