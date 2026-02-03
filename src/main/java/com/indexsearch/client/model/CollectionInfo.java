package com.indexsearch.client.model;

import lombok.Data;

/**
 * Collection listing details returned by the server.
 */
@Data
public class CollectionInfo {
    private String name;
    private long documentCount;
    private long sizeBytes;
    private String sizeHuman;
}
