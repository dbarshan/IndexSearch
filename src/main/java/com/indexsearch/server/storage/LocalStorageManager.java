package com.indexsearch.server.storage;

import org.springframework.stereotype.Service;

import com.indexsearch.server.util.LoggerUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * File-system backed implementation of {@link StorageManager}.
 */
@Service
public class LocalStorageManager implements StorageManager {
    /**
     * Create an empty file, creating parent folders as needed.
     */
    @Override
    public void createFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file: " + filePath, e);
        }
    }

    @Override
    public void createFile(String filePath, String content) {
        createFile(filePath);
        writeFile(filePath, content);
    }

    /**
     * Ensure a folder exists on disk.
     */
    @Override
    public void createFolder(String folderPath) {
        Path path = Paths.get(folderPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder: " + folderPath, e);
        }
    }

    @Override
    public boolean isFileExists(String filePath) {
        return Files.isRegularFile(Paths.get(filePath));
    }

    @Override
    public boolean isDirectoryExists(String directoryPath) {
        return Files.isDirectory(Paths.get(directoryPath));
    }

    /**
     * Write UTF-8 content, creating parent folders as needed.
     */
    @Override
    public void writeFile(String filePath, String content) {
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    @Override
    public String readFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoggerUtil.warn("Failed to read file: {}", filePath);
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    @Override
    public void deleteFolder(String folderPath) {
        Path path = Paths.get(folderPath);
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete path: " + p, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete folder: " + folderPath, e);
        }
    }
}
