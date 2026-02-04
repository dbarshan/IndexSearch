package com.indexsearch.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.service.Processor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for document CRUD within a collection.
 */
@RestController
@RequestMapping("/api/collections/{collection}/documents")
@Profile("server")
public class DocumentController {
    private final Processor processor;

    public DocumentController(Processor processor) {
        this.processor = processor;
    }

    /**
     * Create a new document in the collection.
     */
    @PostMapping
    public ResponseEntity<?> create(@PathVariable String collection, @RequestBody JsonNode document) {
        if (document != null && document.isArray()) {
            return ResponseEntity.ok(processor.createDocuments(collection, document));
        }
        return ResponseEntity.ok(processor.createDocument(collection, document));
    }

    /**
     * Update an existing document by id.
     */
    @PutMapping("/{id}")
    public ResponseEntity<JsonNode> update(@PathVariable String collection,
                                           @PathVariable String id,
                                           @RequestBody JsonNode document) {
        return ResponseEntity.ok(processor.updateDocument(collection, id, document));
    }

    /**
     * Fetch a single document by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> get(@PathVariable String collection, @PathVariable String id) {
        JsonNode doc = processor.getDocument(collection, id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    /**
     * Delete a document by id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String collection, @PathVariable String id) {
        processor.deleteDocument(collection, id);
        return ResponseEntity.ok("Document deleted successfully.");
    }
}
