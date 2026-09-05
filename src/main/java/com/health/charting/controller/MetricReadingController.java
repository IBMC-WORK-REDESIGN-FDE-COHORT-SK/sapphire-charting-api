package com.health.charting.controller;

import com.health.charting.dto.response.MetricReadingsResponse;
import com.health.charting.service.MetricReadingService;
import com.health.charting.service.MetricReadingService.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for metric readings API.
 * Provides tabular access to health metrics with pagination support.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metric Readings API", description = "Endpoints for retrieving health metrics in tabular format")
public class MetricReadingController {
    
    private final MetricReadingService metricReadingService;
    
    /**
     * Get paginated metric readings for a specific metric type and user.
     * Pagination metadata is provided in response headers.
     * 
     * @param metric Metric type (e.g., "blood_pressure", "glucose", "heartrate")
     * @param userId User identifier
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 10, max: 50)
     * @param from Optional start time filter (epoch milliseconds)
     * @param to Optional end time filter (epoch milliseconds)
     * @return MetricReadingsResponse with data and pagination headers
     */
    @GetMapping("/{metric}/readings")
    @Operation(
            summary = "Get metric readings",
            description = "Retrieve paginated metric readings for a specific user and metric type. " +
                    "Supports time-based filtering and returns computed health status for each reading. " +
                    "Pagination metadata is provided in response headers (X-Page-Number, X-Total-Elements, etc.)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Readings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MetricReadingsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters (e.g., invalid metric name, invalid page/size)",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<MetricReadingsResponse> getReadings(
            @Parameter(description = "Metric type identifier", example = "blood_pressure", required = true)
            @PathVariable String metric,
            
            @Parameter(description = "User identifier", example = "u1", required = true)
            @RequestParam String userId,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            
            @Parameter(description = "Page size (max 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            
            @Parameter(description = "Start time filter (epoch milliseconds)", example = "1707292800000")
            @RequestParam(required = false) Long from,
            
            @Parameter(description = "End time filter (epoch milliseconds)", example = "1707379200000")
            @RequestParam(required = false) Long to) {
        
        log.info("GET /api/v1/metrics/{}/readings - userId={}, page={}, size={}, from={}, to={}",
                metric, userId, page, size, from, to);
        
        // Fetch readings
        MetricReadingsResponse response = metricReadingService.fetchReadings(
                metric, userId, page, size, from, to);
        
        // Calculate pagination info
        PageInfo pageInfo = metricReadingService.calculatePageInfo(
                metric, userId, page, size, from, to);
        
        // Build response with pagination headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Page-Number", String.valueOf(pageInfo.pageNumber));
        headers.add("X-Page-Size", String.valueOf(pageInfo.pageSize));
        headers.add("X-Total-Elements", String.valueOf(pageInfo.totalElements));
        headers.add("X-Total-Pages", String.valueOf(pageInfo.totalPages));
        headers.add("X-Has-Next", String.valueOf(pageInfo.hasNext));
        headers.add("X-Has-Previous", String.valueOf(pageInfo.hasPrevious));
        
        log.info("Returning {} readings (page {}/{}, total elements: {})",
                response.getData().size(), pageInfo.pageNumber + 1, 
                pageInfo.totalPages, pageInfo.totalElements);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }
}

// Made with Bob