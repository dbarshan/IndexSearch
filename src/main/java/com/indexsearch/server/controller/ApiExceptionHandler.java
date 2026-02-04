package com.indexsearch.server.controller;

import com.indexsearch.server.exception.CollectionNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps application exceptions to HTTP responses.
 */
@RestControllerAdvice
@Profile("server")
public class ApiExceptionHandler {
    @ExceptionHandler(CollectionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCollectionNotFound(CollectionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
