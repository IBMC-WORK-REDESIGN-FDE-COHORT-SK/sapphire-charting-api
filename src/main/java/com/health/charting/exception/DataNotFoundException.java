package com.health.charting.exception;

/**
 * Exception thrown when no data is found for the given query parameters.
 */
public class DataNotFoundException extends RuntimeException {
    
    public DataNotFoundException(String message) {
        super(message);
    }
    
    public DataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
