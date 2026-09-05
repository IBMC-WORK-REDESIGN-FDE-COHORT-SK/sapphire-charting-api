package com.health.charting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated metric readings.
 * Pagination metadata is provided in HTTP headers rather than the response body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response containing metric readings")
public class MetricReadingsResponse {
    
    @Schema(description = "Metric type identifier", example = "blood_pressure")
    private String metric;
    
    @Schema(description = "User identifier", example = "u1")
    private String userId;
    
    @Schema(description = "List of metric readings for the current page")
    private List<MetricReadingDto> data;
}

// Made with Bob