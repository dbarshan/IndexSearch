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
    /**
     * Optional limit for result size.
     */
    private final Integer limit;
    /**
     * Optional field name for ordering results.
     */
    private final String orderByField;
    /**
     * True if order should be descending when orderByField is set.
     */
    private final boolean orderDesc;
    /**
     * True when all fields should be returned ("*").
     */
    private final boolean selectAll;
    /**
     * Ordered list of selected fields when selectAll is false.
     */
    private final java.util.List<String> selectFields;
}
