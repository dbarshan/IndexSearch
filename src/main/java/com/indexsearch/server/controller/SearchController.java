package com.indexsearch.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.model.QueryRequest;
import com.indexsearch.server.service.Processor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for searching collections.
 */
@RestController
@RequestMapping("/api")
@Profile("server")
public class SearchController {
    private final Processor processor;

    public SearchController(Processor processor) {
        this.processor = processor;
    }

    /**
     * Search a collection with a simple query string.
     */
    @PostMapping("/collections/search")
    public ResponseEntity<List<JsonNode>> search(@RequestParam String collection,
                                                 @RequestParam String query) {
        return ResponseEntity.ok(processor.search(collection, query));
    }

    /**
     * Run a SQL-like query string against the index.
     */
    @PostMapping("/query")
    public ResponseEntity<List<JsonNode>> sqlQuery(@RequestBody QueryRequest request) {
        return ResponseEntity.ok(processor.searchSql(request.getQuery()));
    }
}
