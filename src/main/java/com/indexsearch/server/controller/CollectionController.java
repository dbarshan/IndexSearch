package com.indexsearch.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.collection.CollectionService;
import com.indexsearch.server.model.CollectionInfo;
import com.indexsearch.server.service.MetadataService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for collection creation/deletion.
 */
@RestController
@RequestMapping("/api/collections")
@Profile("server")
public class CollectionController {
    private final CollectionService collectionService;
    private final MetadataService metadataService;

    public CollectionController(CollectionService collectionService,
                                MetadataService metadataService) {
        this.collectionService = collectionService;
        this.metadataService = metadataService;
    }

    /**
     * List all collections known by metadata files.
     */
    @GetMapping
    public ResponseEntity<java.util.List<CollectionInfo>> listCollections() {
        return ResponseEntity.ok(metadataService.listCollectionInfos());
    }

    /**
     * Create a collection based on the provided mapping.
     */
    @PostMapping
    public ResponseEntity<String> createCollection(@RequestBody JsonNode mapping) {
        collectionService.create(mapping);
        return ResponseEntity.ok("Collection created successfully.");
    }

    /**
     * Delete a collection by name.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteCollection(@PathVariable String name) {
        collectionService.delete(name);
        return ResponseEntity.ok("Collection deleted successfully.");
    }
}
