package com.health.charting.exception;

/**
 * Exception thrown when a chart query request is invalid.
 * This includes validation errors, invalid parameters, or malformed queries.
 */
public class InvalidQueryException extends RuntimeException {
    
    public InvalidQueryException(String message) {
        super(message);
    }
    
    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
