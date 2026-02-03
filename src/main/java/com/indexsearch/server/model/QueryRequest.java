package com.indexsearch.server.model;

import lombok.Data;

/**
 * Request payload for SQL-like queries.
 */
@Data
public class QueryRequest {
    /**
     * Raw SQL-like query string.
     */
    private String query;
}
