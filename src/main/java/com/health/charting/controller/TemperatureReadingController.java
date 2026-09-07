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
 * Dedicated REST controller for temperature metric readings.
 *
 * <p>Maps {@code GET /api/v1/metrics/temperature/readings} to
 * {@link MetricReadingService#fetchReadings} with metric type fixed to "temperature".
 * All responses are automatically audit-logged via {@code TemperatureAuditAspect} (T009).
 * Keycloak JWT authentication is enforced by the inherited {@code SecurityConfig}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics/temperature")
@RequiredArgsConstructor
@Tag(name = "Temperature Readings API", description = "Endpoints for retrieving temperature metric readings")
public class TemperatureReadingController {

    private static final String METRIC_TYPE = "temperature";

    private final MetricReadingService metricReadingService;

    /**
     * Get paginated temperature readings for a specific user.
     */
    @GetMapping("/readings")
    @Operation(
            summary = "Get temperature readings",
            description = "Retrieve paginated temperature readings for a specific user. " +
                    "Supports time-based filtering and returns computed health status per reading. " +
                    "Pagination metadata is provided in response headers."
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
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<MetricReadingsResponse> getReadings(
            @Parameter(description = "User identifier", example = "u1", required = true)
            @RequestParam String userId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Page size (max 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,

            @Parameter(description = "Start time filter (epoch milliseconds)")
            @RequestParam(required = false) Long from,

            @Parameter(description = "End time filter (epoch milliseconds)")
            @RequestParam(required = false) Long to) {

        log.info("GET /api/v1/metrics/temperature/readings - userId={}, page={}, size={}, from={}, to={}",
                userId, page, size, from, to);

        MetricReadingsResponse response = metricReadingService.fetchReadings(
                METRIC_TYPE, userId, page, size, from, to);

        PageInfo pageInfo = metricReadingService.calculatePageInfo(
                METRIC_TYPE, userId, page, size, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Page-Number", String.valueOf(pageInfo.pageNumber));
        headers.add("X-Page-Size", String.valueOf(pageInfo.pageSize));
        headers.add("X-Total-Elements", String.valueOf(pageInfo.totalElements));
        headers.add("X-Total-Pages", String.valueOf(pageInfo.totalPages));
        headers.add("X-Has-Next", String.valueOf(pageInfo.hasNext));
        headers.add("X-Has-Previous", String.valueOf(pageInfo.hasPrevious));

        log.info("Returning {} temperature readings (page {}/{}, total: {})",
                response.getData().size(), pageInfo.pageNumber + 1,
                pageInfo.totalPages, pageInfo.totalElements);

        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }
}

// Made with Bob
