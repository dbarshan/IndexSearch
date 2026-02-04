package com.indexsearch.server.exception;

/**
 * Thrown when a requested collection does not exist.
 */
public class CollectionNotFoundException extends RuntimeException {
    public CollectionNotFoundException(String message) {
        super(message);
    }
}
