package com.indexsearch.server.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.service.MetadataService;
import com.indexsearch.server.storage.LocalStorageManager;
import com.indexsearch.server.storage.StorageManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppProperties properties;
    private StorageManager storageManager;
    private MetadataService metadataService;
    private IndexService indexService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setMetadataDir(tempDir.resolve("metadata").toString());
        properties.setIndexDir(tempDir.resolve("index").toString());
        storageManager = new LocalStorageManager();
        metadataService = new MetadataService(properties, storageManager);
        indexService = new IndexService(metadataService, properties, storageManager);
    }

    @Test
    void createIndexAndSearchAcrossFields() throws Exception {
        JsonNode mapping = objectMapper.readTree("{\"name\":\"books\",\"keys\":[{\"field\":\"title\"}]}");
        metadataService.createCollection(mapping);

        List<JsonNode> docs = List.of(
                objectMapper.readTree("{\"id\":\"1\",\"title\":\"Hello World\"}"),
                objectMapper.readTree("{\"id\":\"2\",\"title\":\"Hello There\"}")
        );
        indexService.createIndex("books", docs);

        Set<String> result = indexService.search("books", "hello");
        assertEquals(Set.of("1", "2"), result);
    }

    @Test
    void searchWithUnknownFieldReturnsEmpty() throws Exception {
        JsonNode mapping = objectMapper.readTree("{\"name\":\"books\",\"keys\":[{\"field\":\"title\"}]}");
        metadataService.createCollection(mapping);

        List<JsonNode> docs = List.of(
                objectMapper.readTree("{\"id\":\"1\",\"title\":\"Hello World\"}")
        );
        indexService.createIndex("books", docs);

        Set<String> result = indexService.search("books", "hello", "body");
        assertEquals(Set.of(), result);
    }
}
