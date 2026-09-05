package com.health.charting.controller;

import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.response.ChartResponse;
import com.health.charting.service.ChartQueryService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for chart query operations.
 * Provides endpoints for querying health metrics and returning chart data.
 * Includes custom metrics for monitoring API performance.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/charts")
@RequiredArgsConstructor
@Tag(name = "Chart API", description = "Endpoints for querying and visualizing health metrics")
public class ChartController {
    
    private final ChartQueryService chartQueryService;
    private final Counter chartQueryRequestCounter;
    private final Counter chartQuerySuccessCounter;
    private final Counter chartQueryErrorCounter;
    private final Timer chartQueryTimer;
    private final Counter dataPointsCounter;
    private final MeterRegistry meterRegistry;
    
    /**
     * Query chart data from TimescaleDB.
     * 
     * @param request the chart query request
     * @return ChartResponse containing all series data
     */
    @PostMapping("/query")
    @Operation(
            summary = "Query chart data",
            description = "Execute a chart query against TimescaleDB and return formatted data for visualization. " +
                    "Supports multiple metric types, time-based aggregations, and flexible series specifications."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Query executed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChartResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No data found for the specified query",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<ChartResponse> query(@Valid @RequestBody ChartQueryRequest request) {
        log.info("Received chart query request: userId={}, metricType={}, resolution={}",
                request.getUserId(), request.getMetricType(), request.getResolution());
        
        // Increment total request counter
        chartQueryRequestCounter.increment();
        
        // Record metric type distribution
        meterRegistry.counter("charting.api.requests.by.metric.type",
                "metric_type", request.getMetricType().name()).increment();
        
        // Record resolution distribution
        meterRegistry.counter("charting.api.requests.by.resolution",
                "resolution", request.getResolution().name()).increment();
        
        // Time the query execution
        return chartQueryTimer.record(() -> {
            try {
                ChartResponse response = chartQueryService.query(request);
                
                // Increment success counter
                chartQuerySuccessCounter.increment();
                
                // Record data points returned
                dataPointsCounter.increment(response.getTotalDataPoints());
                
                // Record series count distribution
                meterRegistry.counter("charting.api.series.count",
                        "count", String.valueOf(response.getSeriesCount())).increment();
                
                log.info("Chart query completed: seriesCount={}, totalDataPoints={}",
                        response.getSeriesCount(), response.getTotalDataPoints());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                // Increment error counter
                chartQueryErrorCounter.increment();
                
                // Record error type
                meterRegistry.counter("charting.api.errors.by.type",
                        "error_type", e.getClass().getSimpleName()).increment();
                
                log.error("Chart query failed: {}", e.getMessage(), e);
                throw e;
            }
        });
    }
}

// Made with Bob
