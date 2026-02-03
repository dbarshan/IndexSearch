package com.indexsearch.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for data, metadata, and index locations.
 */
@Data
@ConfigurationProperties(prefix = "application")
public class AppProperties {
    /**
     * Root folder where document JSON files are stored.
     */
    private String dataDir = "data";
    /**
     * Folder where collection mapping metadata is stored.
     */
    private String metadataDir = "metadata";
    /**
     * Folder where index/trie files are stored.
     */
    private String indexDir = "index";
}
