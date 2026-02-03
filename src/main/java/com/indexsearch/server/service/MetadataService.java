package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.storage.StorageManager;
import com.indexsearch.server.util.LoggerUtil;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.indexsearch.server.model.CollectionInfo;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Stores and retrieves collection mapping metadata.
 */
@Service
public class MetadataService {
    private final AppProperties properties;
    private final StorageManager storageManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetadataService(AppProperties properties, StorageManager storageManager) {
        this.properties = properties;
        this.storageManager = storageManager;
    }

    /**
     * Persist a collection mapping to disk.
     */
    public void createCollection(JsonNode mapping) {
        String name = extractName(mapping);
        storageManager.createFolder(properties.getMetadataDir());
        Path mappingPath = Paths.get(properties.getMetadataDir(), name + ".json");
        try {
            objectMapper.writeValue(mappingPath.toFile(), mapping);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write collection mapping: " + mappingPath, e);
        }
        LoggerUtil.info("Collection metadata saved: {}", name);
    }

    /**
     * Delete a collection mapping file.
     */
    public void deleteCollection(String name) {
        Path mappingPath = Paths.get(properties.getMetadataDir(), name + ".json");
        storageManager.deleteFile(mappingPath.toString());
        LoggerUtil.info("Collection metadata deleted: {}", name);
    }

    /**
     * List collection names based on mapping files in the metadata folder.
     */
    public List<String> listCollections() {
        Path metadataDir = Paths.get(properties.getMetadataDir());
        if (!storageManager.isDirectoryExists(metadataDir.toString())) {
            return List.of();
        }
        try (var stream = Files.list(metadataDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list collection metadata files", e);
        }
    }

    /**
     * List collection names along with document counts and storage size.
     */
    public List<CollectionInfo> listCollectionInfos() {
        List<String> names = listCollections();
        List<CollectionInfo> infos = new ArrayList<>();
        for (String name : names) {
            long docCount = countDocuments(name);
            long sizeBytes = computeCollectionSizeBytes(name);
            infos.add(new CollectionInfo(name, docCount, sizeBytes, formatSize(sizeBytes)));
        }
        return infos;
    }

    /**
     * Load a collection mapping by name.
     */
    public JsonNode getMapping(String name) {
        Path mappingPath = Paths.get(properties.getMetadataDir(), name + ".json");
        if (!storageManager.isFileExists(mappingPath.toString())) {
            return null;
        }
        try {
            return objectMapper.readTree(mappingPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read collection mapping: " + mappingPath, e);
        }
    }

    /**
     * Extract indexed fields from the mapping.
     */
    public List<String> getIndexedFields(String name) {
        JsonNode mapping = getMapping(name);
        if (mapping == null) {
            return List.of();
        }
        JsonNode keys = mapping.get("keys");
        if (keys == null || !keys.isArray()) {
            return List.of();
        }
        List<String> fields = new ArrayList<>();
        Iterator<JsonNode> it = keys.elements();
        while (it.hasNext()) {
            JsonNode node = it.next();
            if (node.hasNonNull("field")) {
                fields.add(node.get("field").asText());
            }
        }
        return fields;
    }

    /**
     * Read the collection name from mapping.
     */
    public String getMappingName(JsonNode mapping) {
        return extractName(mapping);
    }

    /**
     * Validate and normalize the collection name from mapping.
     */
    private String extractName(JsonNode mapping) {
        if (mapping == null || !mapping.hasNonNull("name")) {
            throw new IllegalArgumentException("Collection mapping must include non-empty 'name'");
        }
        String name = mapping.get("name").asText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be empty");
        }
        return name;
    }

    private long countDocuments(String collection) {
        Path collectionDir = Paths.get(properties.getDataDir(), collection);
        if (!storageManager.isDirectoryExists(collectionDir.toString())) {
            return 0;
        }
        try (var stream = Files.walk(collectionDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to count documents for collection " + collection, e);
        }
    }

    private long computeCollectionSizeBytes(String collection) {
        long size = 0;
        size += sizeOfPath(Paths.get(properties.getDataDir(), collection));
        size += sizeOfPath(Paths.get(properties.getIndexDir(), collection));
        size += sizeOfPath(Paths.get(properties.getMetadataDir(), collection + ".json"));
        return size;
    }

    private long sizeOfPath(Path path) {
        if (Files.isRegularFile(path)) {
            try {
                return Files.size(path);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read size for " + path, e);
            }
        }
        if (!Files.isDirectory(path)) {
            return 0;
        }
        try (var stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to read size for " + file, e);
                        }
                    })
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read size for " + path, e);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double size = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unitIndex = -1;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.2f %s", size, units[unitIndex]);
    }
}
