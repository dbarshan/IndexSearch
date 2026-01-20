package com.prd.indexsearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileSystemDocumentStorageService implements DocumentStorageService {

    private final String DATA_DIR = "data";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileSystemDocumentStorageService() {
        // Create data directory if it doesn't exist
        File directory = new File(DATA_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public void store(String id, JsonNode content) {
        try {
            Path path = Paths.get(DATA_DIR, id + ".json");
            objectMapper.writeValue(path.toFile(), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store document: " + id, e);
        }
    }

    @Override
    public JsonNode retrieve(String id) {
        try {
            Path path = Paths.get(DATA_DIR, id + ".json");
            if (!Files.exists(path)) {
                return null;
            }
            return objectMapper.readTree(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve document: " + id, e);
        }
    }

    @Override
    public List<JsonNode> retrieveAll() {
        List<JsonNode> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(DATA_DIR))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".json"))
                 .forEach(path -> {
                     try {
                         documents.add(objectMapper.readTree(path.toFile()));
                     } catch (IOException e) {
                         System.err.println("Failed to read file: " + path + " - " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
             throw new RuntimeException("Failed to retrieve all documents", e);
        }
        return documents;
    }
}
