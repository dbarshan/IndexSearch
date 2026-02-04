package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.storage.StorageManager;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists document JSON files to disk and loads them when needed.
 */
@Service
public class DataService {
    private final AppProperties properties;
    private final StorageManager storageManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataService(AppProperties properties, StorageManager storageManager) {
        this.properties = properties;
        this.storageManager = storageManager;
    }

    /**
     * Create a new document with a generated id.
     */
    public JsonNode create(String collection, JsonNode document) {
        String docId = generateId(document);
        writeDocument(collection, docId, document);
        return document;
    }

    /**
     * Update an existing document with a provided id.
     */
    public JsonNode update(String collection, String docId, JsonNode document) {
        if (document instanceof ObjectNode) {
            ((ObjectNode) document).put("id", docId);
        }
        writeDocument(collection, docId, document);
        return document;
    }

    /**
     * Load a document by id; returns null if missing.
     */
    public JsonNode get(String collection, String docId) {
        Path path = Paths.get(properties.getDataDir(), collection, docId + ".json");
        if (!storageManager.isFileExists(path.toString())) {
            return null;
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document: " + path, e);
        }
    }

    /**
     * Delete a document file by id.
     */
    public void delete(String collection, String docId) {
        Path path = Paths.get(properties.getDataDir(), collection, docId + ".json");
        storageManager.deleteFile(path.toString());
    }

    /**
     * Load all documents for a collection.
     */
    public List<JsonNode> listAll(String collection) {
        Path dir = Paths.get(properties.getDataDir(), collection);
        if (!storageManager.isDirectoryExists(dir.toString())) {
            return List.of();
        }
        List<JsonNode> docs = new ArrayList<>();
        try (var stream = java.nio.file.Files.walk(dir)) {
            stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            docs.add(objectMapper.readTree(path.toFile()));
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read document: " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list documents for collection: " + collection, e);
        }
        return docs;
    }

    /**
     * Load up to a limited number of documents for a collection.
     */
    public List<JsonNode> listAll(String collection, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Path dir = Paths.get(properties.getDataDir(), collection);
        if (!storageManager.isDirectoryExists(dir.toString())) {
            return List.of();
        }
        List<JsonNode> docs = new ArrayList<>();
        try (var stream = java.nio.file.Files.walk(dir)) {
            stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .limit(limit)
                    .forEach(path -> {
                        try {
                            docs.add(objectMapper.readTree(path.toFile()));
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read document: " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list documents for collection: " + collection, e);
        }
        return docs;
    }

    /**
     * Write a document JSON file to the collection directory.
     */
    private void writeDocument(String collection, String docId, JsonNode document) {
        Path path = Paths.get(properties.getDataDir(), collection, docId + ".json");
        storageManager.createFolder(path.getParent().toString());
        try {
            objectMapper.writeValue(path.toFile(), document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write document: " + path, e);
        }
    }

    /**
     * Generate and inject a new UUID into the document.
     */
    private String generateId(JsonNode document) {
        String newId = UUID.randomUUID().toString();
        if (document instanceof ObjectNode) {
            ((ObjectNode) document).put("id", newId);
        }
        return newId;
    }
}
