package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.index.IndexService;
import com.indexsearch.server.model.QuerySpec;
import com.indexsearch.server.query.QueryProcessor;
import com.indexsearch.server.util.LoggerUtil;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates document persistence, indexing, and query handling.
 */
@Service
public class Processor {
    private static final int MAX_RESULTS = 200;
    private final DataService dataService;
    private final IndexService indexService;
    private final MetadataService metadataService;
    private final QueryProcessor queryProcessor;

    public Processor(DataService dataService,
                     IndexService indexService,
                     MetadataService metadataService,
                     QueryProcessor queryProcessor) {
        this.dataService = dataService;
        this.indexService = indexService;
        this.metadataService = metadataService;
        this.queryProcessor = queryProcessor;
    }

    /**
     * Create a document and index it.
     */
    public JsonNode createDocument(String collection, JsonNode document) {
        JsonNode created = dataService.create(collection, document);
        String docId = created.get("id").asText();
        indexService.putIndex(collection, docId, created);
        return created;
    }

    /**
     * Create one or more documents and index them.
     */
    public List<JsonNode> createDocuments(String collection, JsonNode payload) {
        List<JsonNode> created = new ArrayList<>();
        if (payload == null) {
            return created;
        }
        if (payload.isArray()) {
            for (JsonNode item : payload) {
                created.add(createDocument(collection, item));
            }
            return created;
        }
        created.add(createDocument(collection, payload));
        return created;
    }

    /**
     * Update a document and refresh its index entries.
     */
    public JsonNode updateDocument(String collection, String docId, JsonNode document) {
        JsonNode updated = dataService.update(collection, docId, document);
        indexService.putIndex(collection, docId, updated);
        return updated;
    }

    /**
     * Load a document by id.
     */
    public JsonNode getDocument(String collection, String docId) {
        return dataService.get(collection, docId);
    }

    /**
     * Delete a document and remove it from the index.
     */
    public void deleteDocument(String collection, String docId) {
        dataService.delete(collection, docId);
        indexService.deleteIndex(collection, docId);
    }

    /**
     * Search documents by query string across indexed fields.
     */
    public List<JsonNode> search(String collection, String query) {
        validateCollection(collection);
        Set<String> docIds = indexService.search(collection, query);
        return resolveDocuments(collection, docIds, MAX_RESULTS);
    }

    /**
     * Search documents using SQL-like query syntax.
     */
    public List<JsonNode> searchSql(String query) {
        QuerySpec spec = queryProcessor.parse(query);
        if (spec == null) {
            throw new IllegalArgumentException("Expected a SQL query");
        }
        validateCollection(spec.getCollectionName());
        if (spec.getQueryText() == null || spec.getQueryText().isBlank()) {
            return dataService.listAll(spec.getCollectionName(), MAX_RESULTS);
        }
        Set<String> docIds = indexService.search(
                spec.getCollectionName(),
                spec.getQueryText(),
                spec.getFieldName()
        );
        return resolveDocuments(spec.getCollectionName(), docIds, MAX_RESULTS);
    }

    public void rebuildIndex(String collection) {
        List<JsonNode> documents = dataService.listAll(collection);
        indexService.createIndex(collection, documents);
        LoggerUtil.info("Index rebuilt for collection {}", collection);
    }

    /**
     * Ensure the collection exists before searching.
     */
    private void validateCollection(String collection) {
        if (metadataService.getMapping(collection) == null) {
            throw new IllegalArgumentException("Collection not found: " + collection);
        }
    }

    /**
     * Convert document ids into document payloads.
     */
    private List<JsonNode> resolveDocuments(String collection, Set<String> docIds, int limit) {
        List<JsonNode> results = new ArrayList<>();
        if (limit <= 0) {
            return results;
        }
        int count = 0;
        for (String docId : docIds) {
            JsonNode doc = dataService.get(collection, docId);
            if (doc != null) {
                results.add(doc);
                count++;
                if (count >= limit) {
                    break;
                }
            }
        }
        return results;
    }
}
