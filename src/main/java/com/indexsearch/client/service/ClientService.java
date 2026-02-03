package com.indexsearch.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.client.model.CollectionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClientService {
    private final RestTemplate restTemplate;

    @Value("${indexsearch.server.url:http://localhost:8080}")
    private String serverBaseUrl;

    public ClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createCollection(JsonNode mapping) {
        restTemplate.postForEntity(serverBaseUrl + "/api/collections", mapping, String.class);
    }

    public void deleteCollection(String name) {
        restTemplate.delete(serverBaseUrl + "/api/collections/{name}", name);
    }

    public List<CollectionInfo> listCollections() {
        ResponseEntity<List<CollectionInfo>> response = restTemplate.exchange(
                serverBaseUrl + "/api/collections",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public JsonNode addDocument(String collection, JsonNode document) {
        return restTemplate.postForEntity(
                serverBaseUrl + "/api/collections/{collection}/documents",
                document,
                JsonNode.class,
                collection
        ).getBody();
    }

    public JsonNode updateDocument(String collection, String docId, JsonNode document) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                serverBaseUrl + "/api/collections/{collection}/documents/{id}",
                HttpMethod.PUT,
                new HttpEntity<JsonNode>(Objects.requireNonNull(document, "document")),
                JsonNode.class,
                collection,
                docId
        );
        return response.getBody();
    }

    public JsonNode getDocument(String collection, String docId) {
        try {
            return restTemplate.getForEntity(
                    serverBaseUrl + "/api/collections/{collection}/documents/{id}",
                    JsonNode.class,
                    collection,
                    docId
            ).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }
    }

    public void deleteDocument(String collection, String docId) {
        restTemplate.delete(
                serverBaseUrl + "/api/collections/{collection}/documents/{id}",
                collection,
                docId
        );
    }

    public List<JsonNode> search(String collection, String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(serverBaseUrl + "/api/collections/search")
                .queryParam("collection", collection)
                .queryParam("query", query)
                .toUriString();
        ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public List<JsonNode> sql(String sqlQuery) {
        Map<String, String> body = Map.of("query", sqlQuery);
        ResponseEntity<List<JsonNode>> response = restTemplate.exchange(
                serverBaseUrl + "/api/query",
                HttpMethod.POST,
                new HttpEntity<Map<String, String>>(Objects.requireNonNull(body, "body")),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}