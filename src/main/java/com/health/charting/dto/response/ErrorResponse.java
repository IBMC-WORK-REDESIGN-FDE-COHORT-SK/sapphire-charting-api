package com.health.charting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard error response DTO.
 * Used for all error responses from the API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {
    
    @Schema(description = "Timestamp of the error", example = "2024-02-07T15:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type", example = "Bad Request")
    private String error;
    
    @Schema(description = "Error message", example = "Invalid query parameters")
    private String message;
    
    @Schema(description = "Request path", example = "/api/v1/charts/query")
    private String path;
    
    @Schema(description = "List of validation errors")
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * Add a validation error to the list.
     * 
     * @param error the error message
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
}

// Made with Bob
