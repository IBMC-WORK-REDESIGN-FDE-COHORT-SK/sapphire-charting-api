package com.health.charting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO representing a single metric reading with timestamp, values, and computed status.
 * Used in the tabular metrics readings API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual metric reading with timestamp, values, and computed status")
public class MetricReadingDto {
    
    @Schema(description = "Reading timestamp in ISO 8601 format", example = "2026-02-07T10:30:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
    
    @Schema(description = "Map of metric component names to their numeric values", 
            example = "{\"systolic\": 120, \"diastolic\": 80}")
    private Map<String, Number> values;
    
    @Schema(description = "Unit of measurement", example = "mmHg")
    private String unit;
    
    @Schema(description = "Source device identifier", example = "device_123")
    private String source;
    
    @Schema(description = "Computed health status based on metric values", 
            example = "NORMAL", 
            allowableValues = {"NORMAL", "ELEVATED", "STAGE_1", "STAGE_2", "CRISIS", "LOW", "HIGH", "CRITICAL", "BRADYCARDIA", "TACHYCARDIA"})
    private String status;
}

// Made with Bob