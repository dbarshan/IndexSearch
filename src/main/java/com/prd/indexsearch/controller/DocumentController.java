package com.prd.indexsearch.controller;

import com.prd.indexsearch.service.InvertedIndexService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final InvertedIndexService invertedIndexService;

    @PostMapping("/documents")
    public ResponseEntity<String> createDocument(@RequestBody JsonNode document, @RequestParam String keyField) {
        invertedIndexService.addDocument(document, keyField);
        return ResponseEntity.ok("Document created/indexed successfully.");
    }

    @GetMapping("/search")
    public ResponseEntity<List<JsonNode>> search(@RequestParam String query) {
        return ResponseEntity.ok(invertedIndexService.search(query));
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<String> updateDocument(@PathVariable String id, @RequestBody JsonNode document, @RequestParam String keyField) {
        invertedIndexService.updateDocument(id, document, keyField);
        return ResponseEntity.ok("Document updated successfully.");
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {
        invertedIndexService.deleteDocument(id);
        return ResponseEntity.ok("Document deleted successfully.");
    }
    
    @GetMapping("/documents/{id}")
    public ResponseEntity<JsonNode> getDocument(@PathVariable String id) {
        JsonNode doc = invertedIndexService.getDocument(id);
        if (doc != null) {
            return ResponseEntity.ok(doc);
        }
        return ResponseEntity.notFound().build();
    }
}
