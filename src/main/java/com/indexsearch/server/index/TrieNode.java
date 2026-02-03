package com.indexsearch.server.index;

import java.util.HashMap;
import java.util.Map;

/**
 * Single trie node storing children, end-of-word flag, and word id.
 */
public class TrieNode {
    private Map<Character, Integer> children = new HashMap<>();
    private boolean endOfWord;
    private String wordId;

    /**
     * Child character -> node index map.
     */
    public Map<Character, Integer> getChildren() {
        return children;
    }

    /**
     * Replace the children map (used when loading).
     */
    public void setChildren(Map<Character, Integer> children) {
        this.children = children;
    }

    /**
     * Whether this node terminates a full token.
     */
    public boolean isEndOfWord() {
        return endOfWord;
    }

    /**
     * Mark this node as an end-of-word node.
     */
    public void setEndOfWord(boolean endOfWord) {
        this.endOfWord = endOfWord;
    }

    /**
     * Stable id associated with the token.
     */
    public String getWordId() {
        return wordId;
    }

    /**
     * Assign a word id when a token is first inserted.
     */
    public void setWordId(String wordId) {
        this.wordId = wordId;
    }
}
