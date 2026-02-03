package com.indexsearch.server.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Binary serializer for persisting {@link TrieStore} to disk.
 */
public final class TrieBinaryStore {
    private TrieBinaryStore() {
    }

    /**
     * Write trie data into a compact binary file.
     */
    public static void write(Path path, TrieStore trie) {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(estimateSize(trie));
            buffer.putInt(trie.getRootIndex());
            buffer.putInt(trie.getNodes().size());
            for (TrieNode node : trie.getNodes()) {
                buffer.put((byte) (node.isEndOfWord() ? 1 : 0));
                String wordId = node.getWordId() == null ? "" : node.getWordId();
                byte[] wordIdBytes = wordId.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(wordIdBytes.length);
                buffer.put(wordIdBytes);
                Map<Character, Integer> children = node.getChildren();
                buffer.putInt(children.size());
                for (Map.Entry<Character, Integer> entry : children.entrySet()) {
                    buffer.putChar(entry.getKey());
                    buffer.putInt(entry.getValue());
                }
            }
            buffer.flip();
            channel.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write trie file: " + path, e);
        }
    }

    /**
     * Read trie data from a binary file into memory.
     */
    public static TrieStore read(Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            int rootIndex = buffer.getInt();
            int nodeCount = buffer.getInt();
            TrieStore trie = new TrieStore();
            trie.getNodes().clear();
            trie.setRootIndex(rootIndex);
            for (int i = 0; i < nodeCount; i++) {
                TrieNode node = new TrieNode();
                node.setEndOfWord(buffer.get() == 1);
                int wordIdLen = buffer.getInt();
                if (wordIdLen > 0) {
                    byte[] wordIdBytes = new byte[wordIdLen];
                    buffer.get(wordIdBytes);
                    node.setWordId(new String(wordIdBytes, StandardCharsets.UTF_8));
                }
                int childCount = buffer.getInt();
                Map<Character, Integer> children = new HashMap<>();
                for (int c = 0; c < childCount; c++) {
                    char key = buffer.getChar();
                    int value = buffer.getInt();
                    children.put(key, value);
                }
                node.setChildren(children);
                trie.getNodes().add(node);
            }
            return trie;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read trie file: " + path, e);
        }
    }

    /**
     * Estimate binary size to avoid buffer reallocation.
     */
    private static int estimateSize(TrieStore trie) {
        int size = Integer.BYTES + Integer.BYTES;
        for (TrieNode node : trie.getNodes()) {
            size += 1;
            String wordId = node.getWordId() == null ? "" : node.getWordId();
            size += Integer.BYTES + wordId.getBytes(StandardCharsets.UTF_8).length;
            size += Integer.BYTES;
            size += node.getChildren().size() * (Character.BYTES + Integer.BYTES);
        }
        return size;
    }
}
