package com.indexsearch.server.util;

import com.indexsearch.server.config.AppProperties;
import com.indexsearch.server.storage.StorageManager;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures required directories exist when the server starts.
 */
@Component
@Profile("server")
public class StartupInitializer {
    private final AppProperties properties;
    private final StorageManager storageManager;

    public StartupInitializer(AppProperties properties, StorageManager storageManager) {
        this.properties = properties;
        this.storageManager = storageManager;
    }

    /**
     * Create data, metadata, index, and log folders if missing.
     */
    public void init() {
        LoggerUtil.info("Startup init in {}", System.getProperty("user.dir"));
        List<String> dirs = List.of(
                properties.getDataDir(),
                properties.getMetadataDir(),
                properties.getIndexDir(),
                "log"
        );
        for (String dir : dirs) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            storageManager.createFolder(dir);
            LoggerUtil.info("Ensured folder exists: {}", dir);
        }
    }
}
