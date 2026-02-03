package com.indexsearch.server.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrieStoreTest {

    @Test
    void addAndFindWord() {
        TrieStore trie = new TrieStore();
        String wordId = trie.addWord("hello");
        assertNotNull(wordId);
        assertEquals(wordId, trie.findWordId("hello"));
    }

    @Test
    void findMissingWordReturnsNull() {
        TrieStore trie = new TrieStore();
        assertNull(trie.findWordId("missing"));
    }
}
