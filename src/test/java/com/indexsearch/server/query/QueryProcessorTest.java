package com.indexsearch.server.query;

import com.indexsearch.server.model.QuerySpec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryProcessorTest {
    private final QueryProcessor processor = new QueryProcessor();

    @Test
    void parseSimpleSelect() {
        QuerySpec spec = processor.parse("select * from books");
        assertNotNull(spec);
        assertEquals("books", spec.getCollectionName());
        assertEquals("", spec.getQueryText());
        assertNull(spec.getFieldName());
    }

    @Test
    void parseSelectWhereFieldEquals() {
        QuerySpec spec = processor.parse("select * from books where title='Hello World'");
        assertNotNull(spec);
        assertEquals("books", spec.getCollectionName());
        assertEquals("Hello World", spec.getQueryText());
        assertEquals("title", spec.getFieldName());
    }

    @Test
    void parseNonSelectReturnsNull() {
        assertNull(processor.parse("show collections"));
    }

    @Test
    void parseEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> processor.parse(" "));
    }
}
