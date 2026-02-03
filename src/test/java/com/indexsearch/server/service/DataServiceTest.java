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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppProperties properties;
    private StorageManager storageManager;
    private DataService dataService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setDataDir(tempDir.resolve("data").toString());
        storageManager = new LocalStorageManager();
        dataService = new DataService(properties, storageManager);
    }

    @Test
    void createGetUpdateDeleteLifecycle() throws Exception {
        JsonNode doc = objectMapper.readTree("{\"title\":\"Hello\"}");
        JsonNode created = dataService.create("books", doc);
        String id = created.get("id").asText();
        assertNotNull(id);

        JsonNode loaded = dataService.get("books", id);
        assertEquals("Hello", loaded.get("title").asText());

        JsonNode updated = objectMapper.readTree("{\"title\":\"Updated\"}");
        dataService.update("books", id, updated);
        JsonNode loadedUpdated = dataService.get("books", id);
        assertEquals("Updated", loadedUpdated.get("title").asText());
        assertEquals(id, loadedUpdated.get("id").asText());

        dataService.delete("books", id);
        assertNull(dataService.get("books", id));
    }
}
