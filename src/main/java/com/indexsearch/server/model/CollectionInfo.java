package com.indexsearch.server.model;

import lombok.Data;

/**
 * Collection listing details returned by the server.
 */
@Data
public class CollectionInfo {
    private final String name;
    private final long documentCount;
    private final long sizeBytes;
    private final String sizeHuman;
}
