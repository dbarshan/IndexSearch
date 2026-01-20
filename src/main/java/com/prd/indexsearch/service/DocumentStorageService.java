package com.prd.indexsearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface DocumentStorageService {
    void store(String id, JsonNode content);
    JsonNode retrieve(String id);
    List<JsonNode> retrieveAll();
}
