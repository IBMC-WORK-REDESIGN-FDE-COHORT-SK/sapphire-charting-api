package com.health.charting.service.impl;

import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.request.SeriesSpec;
import com.health.charting.dto.response.ChartResponse;
import com.health.charting.dto.response.DataPoint;
import com.health.charting.dto.response.Series;
import com.health.charting.enums.ChartType;
import com.health.charting.exception.DataNotFoundException;
import com.health.charting.service.ChartQueryService;
import com.health.charting.util.SqlQueryBuilder;
import com.health.charting.util.SqlQueryBuilder.QueryWithParameters;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ChartQueryService.
 * Executes queries against TimescaleDB and transforms results into chart data.
 * Includes custom metrics for monitoring database query performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartQueryServiceImpl implements ChartQueryService {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqlQueryBuilder sqlQueryBuilder;
    private final Timer databaseQueryTimer;
    private final MeterRegistry meterRegistry;
    
    @Override
    public ChartResponse query(ChartQueryRequest request) {
        log.info("Processing chart query for user: {}, metric: {}, series count: {}",
                request.getUserId(), request.getMetricType(), request.getSeries().size());
        
        // Build response
        ChartResponse response = ChartResponse.builder()
                .chartType(determineChartType(request))
                .xAxis("time")
                .series(new ArrayList<>())
                .build();
        
        // Execute query for each series
        for (SeriesSpec seriesSpec : request.getSeries()) {
            Series series = executeSeriesQuery(request, seriesSpec);
            response.addSeries(series);
        }
        
        // Validate that we have data
        if (response.getTotalDataPoints() == 0) {
            log.warn("No data found for query: user={}, metric={}, timeRange={}-{}",
                    request.getUserId(), request.getMetricType(),
                    request.getTimeRange().from(), request.getTimeRange().to());
            throw new DataNotFoundException(
                    String.format("No data found for user %s and metric %s in the specified time range",
                            request.getUserId(), request.getMetricType()));
        }
        
        log.info("Query completed successfully. Total series: {}, Total data points: {}",
                response.getSeriesCount(), response.getTotalDataPoints());
        
        return response;
    }
    
    /**
     * Execute query for a single series.
     * 
     * @param request the chart query request
     * @param seriesSpec the series specification
     * @return Series with data points
     */
    private Series executeSeriesQuery(ChartQueryRequest request, SeriesSpec seriesSpec) {
        log.debug("Executing query for series: {}", seriesSpec.getName());
        
        // Build SQL query
        QueryWithParameters queryWithParams = sqlQueryBuilder.buildSeriesQuery(request, seriesSpec);
        
        // Record query execution metrics
        meterRegistry.counter("charting.api.database.queries.total",
                "metric_type", request.getMetricType().name(),
                "aggregation", request.getEffectiveAggregation(seriesSpec).name()).increment();
        
        // Time the database query execution
        List<DataPoint> dataPoints = databaseQueryTimer.record(() -> {
            try {
                return jdbcTemplate.query(
                        queryWithParams.sql(),
                        queryWithParams.parameters(),
                        (rs, rowNum) -> {
                            // Extract timestamp
                            long timestamp;
                            Object bucketObj = rs.getObject("bucket");
                            
                            if (bucketObj instanceof Timestamp) {
                                // Time bucket result (timestamp)
                                timestamp = ((Timestamp) bucketObj).getTime();
                            } else if (bucketObj instanceof Long) {
                                // Raw time (bigint)
                                timestamp = (Long) bucketObj;
                            } else {
                                log.warn("Unexpected bucket type: {}", bucketObj.getClass().getName());
                                timestamp = 0L;
                            }
                            
                            // Extract value
                            Double value = rs.getDouble("value");
                            if (rs.wasNull()) {
                                value = null;
                            }
                            
                            return new DataPoint(timestamp, value);
                        }
                );
            } catch (Exception e) {
                // Record database error
                meterRegistry.counter("charting.api.database.errors",
                        "error_type", e.getClass().getSimpleName()).increment();
                throw e;
            }
        });
        
        // Record data points per series
        meterRegistry.counter("charting.api.datapoints.per.series",
                "series_name", seriesSpec.getName()).increment(dataPoints.size());
        
        log.debug("Series '{}' returned {} data points", seriesSpec.getName(), dataPoints.size());
        
        // Build series
        return Series.builder()
                .name(seriesSpec.getName())
                .points(dataPoints)
                .build();
    }
    
    /**
     * Determine the appropriate chart type based on the request.
     *
     * @param request the chart query request
     * @return the chart type
     */
    private ChartType determineChartType(ChartQueryRequest request) {
        // Return the chart type specified in the request, defaulting to LINE if not specified
        return request.getChartType() != null ? request.getChartType() : ChartType.LINE;
    }
}

// Made with Bob
