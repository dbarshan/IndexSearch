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
            Pattern.compile("^select\\s+(.+?)\\s+from\\s+(\\w+)(?:\\s+where\\s+(.+?))?(?:\\s+order\\s+by\\s+(\\w+)(?:\\s+(asc|desc))?)?(?:\\s+limit\\s+(\\d+))?$",
                    Pattern.CASE_INSENSITIVE);
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
        String selectText = matcher.group(1);
        String collection = matcher.group(2);
        String where = matcher.group(3);
        String orderByField = matcher.group(4);
        String orderDir = matcher.group(5);
        String limitText = matcher.group(6);
        String queryText = where == null ? "" : where.trim();
        String fieldName = null;
        Integer limit = null;
        boolean orderDesc = false;
        boolean selectAll = false;
        java.util.List<String> selectFields = java.util.List.of();
        String selectNormalized = selectText == null ? "" : selectText.trim();
        if (selectNormalized.isEmpty()) {
            throw new IllegalArgumentException("Select fields cannot be empty");
        }
        if ("*".equals(selectNormalized)) {
            selectAll = true;
        } else {
            String[] parts = selectNormalized.split(",");
            java.util.List<String> fields = new java.util.ArrayList<>();
            for (String part : parts) {
                String field = part.trim();
                if (!field.isEmpty()) {
                    fields.add(field);
                }
            }
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("Select fields cannot be empty");
            }
            selectFields = java.util.List.copyOf(fields);
        }
        if (!queryText.isEmpty()) {
            Matcher fieldMatcher = FIELD_EQUALS.matcher(queryText);
            if (fieldMatcher.matches()) {
                fieldName = fieldMatcher.group(1);
                queryText = fieldMatcher.group(3);
            }
        }
        if (orderByField != null && orderByField.isBlank()) {
            throw new IllegalArgumentException("Order by field cannot be empty");
        }
        if (orderDir != null) {
            orderDesc = "desc".equalsIgnoreCase(orderDir.trim());
        }
        if (limitText != null) {
            try {
                limit = Integer.parseInt(limitText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid limit value");
            }
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than zero");
            }
        }
        return new QuerySpec(collection, queryText, fieldName, limit, orderByField, orderDesc, selectAll, selectFields);
    }
}
