package com.indexsearch.server.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Binary serializer for postings lists (wordId -> docIds).
 */
public final class PostingsBinaryStore {
    private PostingsBinaryStore() {
    }

    /**
     * Persist postings to a compact binary file.
     */
    public static void write(Path path, Map<String, Set<String>> postings) {
        long estimatedSize = estimateSize(postings);
        if (estimatedSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("Postings file too large to memory-map: " + path);
        }
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            channel.truncate(estimatedSize);
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, estimatedSize);
            buffer.putInt(postings.size());
            for (Map.Entry<String, Set<String>> entry : postings.entrySet()) {
                writeString(buffer, entry.getKey());
                Set<String> docIds = entry.getValue();
                buffer.putInt(docIds.size());
                for (String docId : docIds) {
                    writeString(buffer, docId);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write postings file: " + path, e);
        }
    }

    /**
     * Load postings from a binary file.
     */
    public static Map<String, Set<String>> read(Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            int entryCount = buffer.getInt();
            Map<String, Set<String>> postings = new HashMap<>();
            for (int i = 0; i < entryCount; i++) {
                String wordId = readString(buffer);
                int docCount = buffer.getInt();
                Set<String> docIds = new HashSet<>();
                for (int d = 0; d < docCount; d++) {
                    docIds.add(readString(buffer));
                }
                postings.put(wordId, docIds);
            }
            return postings;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read postings file: " + path, e);
        }
    }

    private static void writeString(ByteBuffer buffer, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static long estimateSize(Map<String, Set<String>> postings) {
        long size = Integer.BYTES;
        for (Map.Entry<String, Set<String>> entry : postings.entrySet()) {
            size += Integer.BYTES + utf8Size(entry.getKey());
            size += Integer.BYTES;
            for (String docId : entry.getValue()) {
                size += Integer.BYTES + utf8Size(docId);
            }
        }
        return size;
    }

    private static int utf8Size(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }
}
