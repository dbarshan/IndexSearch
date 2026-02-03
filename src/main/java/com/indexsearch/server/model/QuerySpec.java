package com.indexsearch.server.model;

import lombok.Data;

/**
 * Parsed query specification from a SQL-like statement.
 */
@Data
public class QuerySpec {
    /**
     * Target collection name.
     */
    private final String collectionName;
    /**
     * Search text extracted from the where clause.
     */
    private final String queryText;
    /**
     * Optional field name for field-specific queries.
     */
    private final String fieldName;
}
