package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.storage.LocalStorageManager;
import com.indexsearch.server.storage.StorageManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetadataServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppProperties properties;
    private StorageManager storageManager;
    private MetadataService metadataService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setMetadataDir(tempDir.resolve("metadata").toString());
        storageManager = new LocalStorageManager();
        metadataService = new MetadataService(properties, storageManager);
    }

    @Test
    void createGetAndDeleteMapping() throws Exception {
        JsonNode mapping = objectMapper.readTree("{\"name\":\"books\",\"keys\":[{\"field\":\"title\"}]}");
        metadataService.createCollection(mapping);

        JsonNode loaded = metadataService.getMapping("books");
        assertNotNull(loaded);
        assertEquals("books", loaded.get("name").asText());
        assertEquals(List.of("title"), metadataService.getIndexedFields("books"));

        metadataService.deleteCollection("books");
        assertNull(metadataService.getMapping("books"));
    }
}
