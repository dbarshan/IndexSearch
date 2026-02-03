package com.indexsearch.server.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.service.MetadataService;
import com.indexsearch.server.storage.StorageManager;
import com.indexsearch.server.util.LoggerUtil;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builds, stores, and queries inverted indexes for collections.
 */
@Service
public class IndexService {
    private final MetadataService metadataService;
    private final AppProperties properties;
    private final StorageManager storageManager;
    /**
     * Bundle of trie + postings map for a single indexed field.
     */
    private static class FieldIndex {
        private final TrieStore trie;
        private final Map<String, Set<String>> postings;

        private FieldIndex(TrieStore trie, Map<String, Set<String>> postings) {
            this.trie = trie;
            this.postings = postings;
        }
    }

    private final Map<String, Map<String, FieldIndex>> collectionIndex = new ConcurrentHashMap<>();
    private final Set<String> stopWords = Set.of("a", "an", "the", "and", "or", "not", "to", "of", "in", "on");

    public IndexService(MetadataService metadataService, AppProperties properties, StorageManager storageManager) {
        this.metadataService = metadataService;
        this.properties = properties;
        this.storageManager = storageManager;
    }

    /**
     * Create an index from scratch for the given collection.
     */
    public void createIndex(String collectionName, List<JsonNode> documents) {
        Map<String, FieldIndex> index = new ConcurrentHashMap<>();
        collectionIndex.put(collectionName, index);
        for (JsonNode document : documents) {
            String docId = document.hasNonNull("id") ? document.get("id").asText() : null;
            if (docId != null) {
                addToIndex(index, collectionName, docId, document);
            }
        }
        saveIndex(collectionName, index);
        LoggerUtil.info("Index created for collection {}", collectionName);
    }

    /**
     * Add or update a single document in the index.
     */
    public void putIndex(String collectionName, String docId, JsonNode document) {
        Map<String, FieldIndex> index = loadIndexIfNeeded(collectionName);
        addToIndex(index, collectionName, docId, document);
        saveIndex(collectionName, index);
    }

    /**
     * Remove a document id from the index.
     */
    public void deleteIndex(String collectionName, String docId) {
        Map<String, FieldIndex> index = loadIndexIfNeeded(collectionName);
        if (index == null) {
            return;
        }
        for (FieldIndex fieldIndex : index.values()) {
            var iterator = fieldIndex.postings.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                Set<String> docIds = entry.getValue();
                if (docIds.remove(docId) && docIds.isEmpty()) {
                    iterator.remove();
                    fieldIndex.trie.removeWordId(entry.getKey());
                }
            }
        }
        saveIndex(collectionName, index);
    }

    /**
     * Search across all indexed fields.
     */
    public Set<String> search(String collectionName, String query) {
        Map<String, FieldIndex> index = loadIndexIfNeeded(collectionName);
        if (index == null || query == null || query.isBlank()) {
            return Set.of();
        }
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return Set.of();
        }
        return searchAcrossFields(index, tokens);
    }

    /**
     * Search within a specific indexed field.
     */
    public Set<String> search(String collectionName, String query, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return search(collectionName, query);
        }
        Map<String, FieldIndex> index = loadIndexIfNeeded(collectionName);
        if (index == null || query == null || query.isBlank()) {
            return Set.of();
        }
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return Set.of();
        }
        FieldIndex fieldIndex = index.get(fieldName);
        if (fieldIndex == null) {
            return Set.of();
        }
        return searchField(fieldIndex, tokens);
    }

    /**
     * Tokenize and add a document's terms into the in-memory index.
     */
    private void addToIndex(Map<String, FieldIndex> index,
                            String collectionName,
                            String docId,
                            JsonNode document) {
        List<String> fields = metadataService.getIndexedFields(collectionName);
        if (fields.isEmpty()) {
            FieldIndex allIndex = index.computeIfAbsent("_all", key -> new FieldIndex(new TrieStore(), new ConcurrentHashMap<>()));
            for (String token : extractAllTextTokens(document)) {
                String wordId = allIndex.trie.addWord(token);
                if (wordId != null) {
                    allIndex.postings.computeIfAbsent(wordId, key -> ConcurrentHashMap.newKeySet()).add(docId);
                }
            }
            return;
        }
        for (String field : fields) {
            if (!document.hasNonNull(field)) {
                continue;
            }
            FieldIndex fieldIndex = index.computeIfAbsent(field, key -> new FieldIndex(new TrieStore(), new ConcurrentHashMap<>()));
            for (String token : tokenize(document.get(field).asText())) {
                String wordId = fieldIndex.trie.addWord(token);
                if (wordId != null) {
                    fieldIndex.postings.computeIfAbsent(wordId, key -> ConcurrentHashMap.newKeySet()).add(docId);
                }
            }
        }
    }

    /**
     * Lazily load index structures from disk for a collection.
     */
    private Map<String, FieldIndex> loadIndexIfNeeded(String collectionName) {
        return collectionIndex.computeIfAbsent(collectionName, key -> loadIndex(collectionName));
    }

    private Map<String, FieldIndex> loadIndex(String collectionName) {
        List<String> fields = metadataService.getIndexedFields(collectionName);
        Map<String, FieldIndex> index = new ConcurrentHashMap<>();
        if (fields.isEmpty()) {
            fields = List.of("_all");
        }
        for (String field : fields) {
            TrieStore trie = loadTrie(collectionName, field);
            Map<String, Set<String>> postings = loadPostings(collectionName, field);
            index.put(field, new FieldIndex(trie, postings));
        }
        return index;
    }

    /**
     * Persist the index to disk (postings + trie per field).
     */
    private void saveIndex(String collectionName, Map<String, FieldIndex> index) {
        for (Map.Entry<String, FieldIndex> entry : index.entrySet()) {
            savePostings(collectionName, entry.getKey(), entry.getValue().postings);
            saveTrie(collectionName, entry.getKey(), entry.getValue().trie);
        }
    }

    private Path getIndexPath(String collectionName, String field) {
        return Paths.get(properties.getIndexDir(), collectionName, field + ".index.bin");
    }

    private Path getTriePath(String collectionName, String field) {
        return Paths.get(properties.getIndexDir(), collectionName, field + ".trie.bin");
    }

    /**
     * Intersect postings across all fields for all tokens.
     */
    private Set<String> searchAcrossFields(Map<String, FieldIndex> index, List<String> tokens) {
        Set<String> result = null;
        for (String token : tokens) {
            Set<String> docIdsForToken = new HashSet<>();
            for (FieldIndex fieldIndex : index.values()) {
                String wordId = fieldIndex.trie.findWordId(token);
                if (wordId != null) {
                    docIdsForToken.addAll(fieldIndex.postings.getOrDefault(wordId, Set.of()));
                }
            }
            if (result == null) {
                result = new HashSet<>(docIdsForToken);
            } else {
                result.retainAll(docIdsForToken);
            }
        }
        return result == null ? Set.of() : result;
    }

    /**
     * Intersect postings for tokens within a single field.
     */
    private Set<String> searchField(FieldIndex fieldIndex, List<String> tokens) {
        Set<String> result = null;
        for (String token : tokens) {
            String wordId = fieldIndex.trie.findWordId(token);
            Set<String> docIds = wordId == null ? Set.of() : fieldIndex.postings.getOrDefault(wordId, Set.of());
            if (result == null) {
                result = new HashSet<>(docIds);
            } else {
                result.retainAll(docIds);
            }
        }
        return result == null ? Set.of() : result;
    }

    /**
     * Load the trie for a field, falling back to empty on errors.
     */
    private TrieStore loadTrie(String collectionName, String field) {
        Path triePath = getTriePath(collectionName, field);
        if (!storageManager.isFileExists(triePath.toString())) {
            return new TrieStore();
        }
        try {
            return TrieBinaryStore.read(triePath);
        } catch (RuntimeException e) {
            LoggerUtil.warn("Failed to load trie file: {}", triePath);
            return new TrieStore();
        }
    }

    private void saveTrie(String collectionName, String field, TrieStore trie) {
        Path triePath = getTriePath(collectionName, field);
        storageManager.createFolder(triePath.getParent().toString());
        TrieBinaryStore.write(triePath, trie);
    }

    /**
     * Load postings for a field from disk, or return empty.
     */
    private Map<String, Set<String>> loadPostings(String collectionName, String field) {
        Path indexPath = getIndexPath(collectionName, field);
        if (!storageManager.isFileExists(indexPath.toString())) {
            return new ConcurrentHashMap<>();
        }
        try {
            Map<String, Set<String>> loaded = PostingsBinaryStore.read(indexPath);
            return new ConcurrentHashMap<>(loaded);
        } catch (RuntimeException e) {
            LoggerUtil.warn("Failed to load index file: {}", indexPath);
            return new ConcurrentHashMap<>();
        }
    }

    private void savePostings(String collectionName, String field, Map<String, Set<String>> postings) {
        Path indexPath = getIndexPath(collectionName, field);
        storageManager.createFolder(indexPath.getParent().toString());
        PostingsBinaryStore.write(indexPath, postings);
    }

    /**
     * Extract tokens from all textual fields of a document.
     */
    private List<String> extractAllTextTokens(JsonNode document) {
        List<String> values = new ArrayList<>();
        document.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                values.add(entry.getValue().asText());
            }
        });
        return values.stream().flatMap(value -> tokenize(value).stream()).collect(Collectors.toList());
    }

    /**
     * Normalize and split text into terms, removing stop-words.
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .filter(token -> !stopWords.contains(token))
                .collect(Collectors.toList());
    }
}
