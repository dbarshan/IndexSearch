package com.indexsearch.server.index;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory trie used to map tokens to stable word ids.
 */
public class TrieStore {
    private int rootIndex = 0;
    private List<TrieNode> nodes = new ArrayList<>();

    /**
     * Initialize with a single root node.
     */
    public TrieStore() {
        nodes.add(new TrieNode());
    }

    /**
     * Return the root node index.
     */
    public int getRootIndex() {
        return rootIndex;
    }

    /**
     * Set the root node index (used when loading).
     */
    public void setRootIndex(int rootIndex) {
        this.rootIndex = rootIndex;
    }

    /**
     * Return all trie nodes.
     */
    public List<TrieNode> getNodes() {
        return nodes;
    }

    /**
     * Replace the node list (used when loading).
     */
    public void setNodes(List<TrieNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Add a token to the trie and return its word id.
     */
    public String addWord(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        int current = rootIndex;
        for (char ch : word.toCharArray()) {
            TrieNode node = nodes.get(current);
            Integer nextIndex = node.getChildren().get(ch);
            if (nextIndex == null) {
                TrieNode child = new TrieNode();
                nodes.add(child);
                nextIndex = nodes.size() - 1;
                node.getChildren().put(ch, nextIndex);
            }
            current = nextIndex;
        }
        TrieNode leaf = nodes.get(current);
        if (!leaf.isEndOfWord()) {
            leaf.setEndOfWord(true);
            leaf.setWordId(UUID.randomUUID().toString());
        }
        return leaf.getWordId();
    }

    /**
     * Look up a word id for an existing token.
     */
    public String findWordId(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        int current = rootIndex;
        for (char ch : word.toCharArray()) {
            TrieNode node = nodes.get(current);
            Integer nextIndex = node.getChildren().get(ch);
            if (nextIndex == null) {
                return null;
            }
            current = nextIndex;
        }
        TrieNode leaf = nodes.get(current);
        return leaf.isEndOfWord() ? leaf.getWordId() : null;
    }

    /**
     * Remove a word id from the trie and prune empty branches.
     */
    public void removeWordId(String wordId) {
        if (wordId == null || wordId.isBlank()) {
            return;
        }
        pruneFromNode(rootIndex, wordId);
    }

    private boolean pruneFromNode(int nodeIndex, String wordId) {
        TrieNode node = nodes.get(nodeIndex);
        if (wordId.equals(node.getWordId())) {
            node.setEndOfWord(false);
            node.setWordId(null);
        }
        var iterator = node.getChildren().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int childIndex = entry.getValue();
            if (pruneFromNode(childIndex, wordId)) {
                iterator.remove();
            }
        }
        return nodeIndex != rootIndex && node.getChildren().isEmpty() && !node.isEndOfWord();
    }
}
