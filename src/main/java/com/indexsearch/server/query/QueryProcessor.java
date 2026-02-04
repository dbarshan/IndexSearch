package com.indexsearch.server.query;

import org.springframework.stereotype.Service;

import com.indexsearch.server.model.QuerySpec;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a small SQL-like syntax into a {@link QuerySpec}.
 */
@Service
public class QueryProcessor {
    private static final Pattern SIMPLE_SQL =
            Pattern.compile("^select\\s+.+\\s+from\\s+(\\w+)(?:\\s+where\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_EQUALS =
            Pattern.compile("^(\\w+)\\s*=\\s*(['\"]?)(.+?)\\2$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse a SQL-like query and extract collection, field, and search text.
     */
    public QuerySpec parse(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        String normalized = query.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("select")) {
            return null;
        }
        Matcher matcher = SIMPLE_SQL.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query syntax");
        }
        String collection = matcher.group(1);
        String where = matcher.group(2);
        String queryText = where == null ? "" : where.trim();
        String fieldName = null;
        if (!queryText.isEmpty()) {
            Matcher fieldMatcher = FIELD_EQUALS.matcher(queryText);
            if (fieldMatcher.matches()) {
                fieldName = fieldMatcher.group(1);
                queryText = fieldMatcher.group(3);
            }
        }
        return new QuerySpec(collection, queryText, fieldName);
    }
}
