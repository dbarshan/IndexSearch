package com.indexsearch.server.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.service.MetadataService;
import com.indexsearch.server.storage.StorageManager;
import com.indexsearch.server.util.LoggerUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles lifecycle operations for collections and their metadata.
 */
@Service
@RequiredArgsConstructor
public class CollectionService {
    private final AppProperties properties;
    private final StorageManager storageManager;
    private final MetadataService metadataService;

    /**
     * Create the collection folder and persist mapping metadata.
     */
    public void create(JsonNode mapping) {
        String name = metadataService.getMappingName(mapping);
        Path collectionPath = Paths.get(properties.getDataDir(), name);
        storageManager.createFolder(collectionPath.toString());
        metadataService.createCollection(mapping);
        LoggerUtil.info("Collection created: {}", name);
    }

    /**
     * Delete the collection folder and its mapping metadata.
     */
    public void delete(String name) {
        Path collectionPath = Paths.get(properties.getDataDir(), name);
        storageManager.deleteFolder(collectionPath.toString());
        metadataService.deleteCollection(name);
        LoggerUtil.info("Collection deleted: {}", name);
    }
}
