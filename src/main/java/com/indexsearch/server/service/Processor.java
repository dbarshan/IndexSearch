package com.indexsearch.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.index.IndexService;
import com.indexsearch.server.exception.CollectionNotFoundException;
import com.indexsearch.server.model.QuerySpec;
import com.indexsearch.server.query.QueryProcessor;
import com.indexsearch.server.util.LoggerUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.ArrayList;

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
        int limit = spec.getLimit() == null ? MAX_RESULTS : Math.min(spec.getLimit(), MAX_RESULTS);
        if (limit <= 0) {
            return List.of();
        }
        boolean hasOrder = spec.getOrderByField() != null && !spec.getOrderByField().isBlank();
        if (spec.getQueryText() == null || spec.getQueryText().isBlank()) {
            List<JsonNode> results = dataService.listAll(
                    spec.getCollectionName(),
                    hasOrder ? MAX_RESULTS : limit
            );
            if (hasOrder) {
                sortResults(results, spec.getOrderByField(), spec.isOrderDesc());
                results = applyLimit(results, limit);
                return applyProjection(results, spec);
            }
            return applyProjection(results, spec);
        }
        Set<String> docIds = indexService.search(
                spec.getCollectionName(),
                spec.getQueryText(),
                spec.getFieldName()
        );
        List<JsonNode> results = resolveDocuments(
                spec.getCollectionName(),
                docIds,
                hasOrder ? MAX_RESULTS : limit
        );
        if (hasOrder) {
            sortResults(results, spec.getOrderByField(), spec.isOrderDesc());
            results = applyLimit(results, limit);
            return applyProjection(results, spec);
        }
        return applyProjection(results, spec);
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
            throw new CollectionNotFoundException("Collection not found: " + collection);
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

    private void sortResults(List<JsonNode> results, String orderByField, boolean desc) {
        if (results == null || results.size() < 2) {
            return;
        }
        Comparator<JsonNode> comparator = (left, right) -> compareOrderValues(
                left == null ? null : left.get(orderByField),
                right == null ? null : right.get(orderByField)
        );
        if (desc) {
            comparator = comparator.reversed();
        }
        results.sort(comparator);
    }

    private int compareOrderValues(JsonNode left, JsonNode right) {
        if (left == null || left.isNull()) {
            return right == null || right.isNull() ? 0 : 1;
        }
        if (right == null || right.isNull()) {
            return -1;
        }
        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue());
        }
        if (left.isBoolean() && right.isBoolean()) {
            return Boolean.compare(left.asBoolean(), right.asBoolean());
        }
        return left.asText().compareToIgnoreCase(right.asText());
    }

    private List<JsonNode> applyLimit(List<JsonNode> results, int limit) {
        if (results == null || results.isEmpty() || limit >= results.size()) {
            return results == null ? List.of() : results;
        }
        return new ArrayList<>(results.subList(0, limit));
    }

    private List<JsonNode> applyProjection(List<JsonNode> results, QuerySpec spec) {
        if (spec.isSelectAll() || results == null || results.isEmpty()) {
            return results == null ? List.of() : results;
        }
        List<JsonNode> projected = new ArrayList<>(results.size());
        for (JsonNode doc : results) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            for (String field : spec.getSelectFields()) {
                JsonNode value = doc == null ? null : doc.get(field);
                node.set(field, value == null ? NullNode.instance : value);
            }
            projected.add(node);
        }
        return projected;
    }
}
