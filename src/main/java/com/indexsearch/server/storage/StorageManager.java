package com.indexsearch.server.storage;

/**
 * Abstraction over filesystem operations used by the server services.
 */
public interface StorageManager {
    /**
     * Create an empty file if it does not exist.
     */
    void createFile(String filePath);
    /**
     * Create a file and write initial content.
     */
    void createFile(String filePath, String content);
    /**
     * Create a directory path (including parents) if missing.
     */
    void createFolder(String folderPath);

    /**
     * Check for a regular file at the given path.
     */
    boolean isFileExists(String filePath);
    /**
     * Check for a directory at the given path.
     */
    boolean isDirectoryExists(String directoryPath);

    /**
     * Write text content to a file.
     */
    void writeFile(String filePath, String content);
    /**
     * Read text content from a file.
     */
    String readFile(String filePath);

    /**
     * Delete a single file if it exists.
     */
    void deleteFile(String filePath);
    /**
     * Delete a directory and its contents recursively.
     */
    void deleteFolder(String folderPath);
}
