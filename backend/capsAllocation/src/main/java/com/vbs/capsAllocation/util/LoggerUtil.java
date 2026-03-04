package com.vbs.capsAllocation.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for structured logging
 */
public class LoggerUtil {
    
    private static final Logger errorLogger = LoggerFactory.getLogger("com.vbs.capsAllocation.error");
    private static final Logger debugLogger = LoggerFactory.getLogger("com.vbs.capsAllocation.debug");
    
    /**
     * Log error messages to error log file
     */
    public static void logError(String message, Object... args) {
        errorLogger.error(message, args);
    }
    
    /**
     * Log error with exception
     */
    public static void logError(String message, Throwable throwable) {
        errorLogger.error(message, throwable);
    }
    
    /**
     * Log debug messages to debug log file
     */
    public static void logDebug(String message, Object... args) {
        debugLogger.debug(message, args);
    }
    
    /**
     * Log info messages
     */
    public static void logInfo(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).info(message, args);
    }
    
    /**
     * Log warning messages
     */
    public static void logWarn(Class<?> clazz, String message, Object... args) {
        LoggerFactory.getLogger(clazz).warn(message, args);
    }
    
    /**
     * Log method entry for debugging
     */
    public static void logMethodEntry(Class<?> clazz, String methodName, Object... params) {
        debugLogger.debug("Entering method: {}.{} with params: {}", clazz.getSimpleName(), methodName, params);
    }
    
    /**
     * Log method exit for debugging
     */
    public static void logMethodExit(Class<?> clazz, String methodName, Object result) {
        debugLogger.debug("Exiting method: {}.{} with result: {}", clazz.getSimpleName(), methodName, result);
    }
    
    /**
     * Log method exit for debugging (void methods)
     */
    public static void logMethodExit(Class<?> clazz, String methodName) {
        debugLogger.debug("Exiting method: {}.{}", clazz.getSimpleName(), methodName);
    }
}
