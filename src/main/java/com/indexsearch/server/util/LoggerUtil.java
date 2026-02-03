package com.indexsearch.server.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Convenience logging helpers that resolve the caller class at runtime.
 */
@UtilityClass
public class LoggerUtil {
    /**
     * Log a debug message using the caller's logger.
     */
    public void debug(String message, Object... args) {
        getLogger().debug(message, args);
    }

    /**
     * Log an info message using the caller's logger.
     */
    public void info(String message, Object... args) {
        getLogger().info(message, args);
    }

    /**
     * Log a warning message using the caller's logger.
     */
    public void warn(String message, Object... args) {
        getLogger().warn(message, args);
    }

    /**
     * Log an error message using the caller's logger.
     */
    public void error(String message, Object... args) {
        getLogger().error(message, args);
    }

    /**
     * Log an error with a throwable using the caller's logger.
     */
    public void error(String message, Throwable throwable) {
        getLogger().error(message, throwable);
    }

    /**
     * Resolve an slf4j logger for the first non-utility class in the stack.
     */
    private Logger getLogger() {
        return LoggerFactory.getLogger(resolveCallerName());
    }

    /**
     * Find the calling class name to associate logs with the caller.
     */
    private String resolveCallerName() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::getClassName)
                .filter(name -> !name.equals(LoggerUtil.class.getName()))
                .filter(name -> !name.equals(Thread.class.getName()))
                .findFirst()
                .orElse(LoggerUtil.class.getName());
    }
}
