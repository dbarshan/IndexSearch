package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.index.IndexService;
import com.indexsearch.server.query.QueryProcessor;
import com.indexsearch.server.storage.LocalStorageManager;
import com.indexsearch.server.storage.StorageManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Processor processor;
    private MetadataService metadataService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setDataDir(tempDir.resolve("data").toString());
        properties.setMetadataDir(tempDir.resolve("metadata").toString());
        properties.setIndexDir(tempDir.resolve("index").toString());

        StorageManager storageManager = new LocalStorageManager();
        metadataService = new MetadataService(properties, storageManager);
        DataService dataService = new DataService(properties, storageManager);
        IndexService indexService = new IndexService(metadataService, properties, storageManager);
        QueryProcessor queryProcessor = new QueryProcessor();
        processor = new Processor(dataService, indexService, metadataService, queryProcessor);
    }

    @Test
    void createSearchSqlAndDelete() throws Exception {
        JsonNode mapping = objectMapper.readTree("{\"name\":\"books\",\"keys\":[{\"field\":\"title\"}]}");
        metadataService.createCollection(mapping);

        JsonNode created = processor.createDocument(
                "books",
                objectMapper.readTree("{\"title\":\"Spring Boot\"}")
        );
        String id = created.get("id").asText();

        List<JsonNode> results = processor.search("books", "spring");
        assertEquals(1, results.size());
        assertEquals(id, results.get(0).get("id").asText());

        List<JsonNode> sqlResults = processor.searchSql("select * from books where title='spring'");
        assertEquals(1, sqlResults.size());

        processor.deleteDocument("books", id);
        assertEquals(0, processor.search("books", "spring").size());
    }
}
