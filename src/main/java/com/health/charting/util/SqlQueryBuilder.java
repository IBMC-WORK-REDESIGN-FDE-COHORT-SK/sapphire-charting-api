package com.health.charting.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.request.SeriesSpec;
import com.health.charting.enums.Aggregation;
import com.health.charting.enums.Resolution;
import com.health.charting.exception.InvalidQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for building safe SQL queries dynamically.
 * Prevents SQL injection by using parameterized queries and validating all inputs.
 */
@Slf4j
@Component
public class SqlQueryBuilder {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${app.database.schema:public}")
    private String databaseSchema;
    
    /**
     * Build a SQL query for a single series.
     * 
     * @param request the chart query request
     * @param seriesSpec the series specification
     * @return QueryWithParameters containing SQL and parameters
     */
    public QueryWithParameters buildSeriesQuery(ChartQueryRequest request, SeriesSpec seriesSpec) {
        validateInputs(request, seriesSpec);
        
        StringBuilder sql = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        // SELECT clause with aggregation
        sql.append("SELECT ");
        
        // Add time bucket or raw time
        // Note: time column is in nanoseconds, convert to seconds for to_timestamp
        if (request.requiresTimeBucket()) {
            String timeBucketSql = request.getResolution()
                    .getTimeBucketSql("to_timestamp(time/1000000000.0)");
            sql.append(timeBucketSql).append(" AS bucket, ");
        } else {
            sql.append("time AS bucket, ");
        }
        
        // Add aggregation function
        Aggregation aggregation = request.getEffectiveAggregation(seriesSpec);
        sql.append(aggregation.getSqlFunction()).append(" AS value ");
        
        // FROM clause with schema
        sql.append("FROM ").append(databaseSchema).append(".")
           .append(request.getMetricType().getTableName()).append(" ");
        
        // WHERE clause
        sql.append("WHERE user_id = :userId ");
        params.addValue("userId", request.getUserId());
        
        // Time range filter
        sql.append("AND time >= :fromTime ");
        sql.append("AND time <= :toTime ");
        params.addValue("fromTime", request.getTimeRange().from());
        params.addValue("toTime", request.getTimeRange().to());
        
        // Metric name filter
        sql.append("AND metric_name = :metricName ");
        params.addValue("metricName", seriesSpec.getMetricName());
        
        // Attribute filters (JSONB)
        if (seriesSpec.hasAttributeFilters()) {
            String jsonbFilter = buildJsonbFilter(seriesSpec.getAttributeFilter());
            sql.append("AND attributes @> :attributeFilter::jsonb ");
            params.addValue("attributeFilter", jsonbFilter);
        }
        
        // GROUP BY clause (only if using time bucketing)
        if (request.requiresTimeBucket()) {
            sql.append("GROUP BY bucket ");
        }
        
        // ORDER BY clause
        sql.append("ORDER BY bucket");
        
        String finalSql = sql.toString();
        log.debug("Generated SQL: {}", finalSql);
        log.debug("Parameters: {}", params.getValues());
        
        return new QueryWithParameters(finalSql, params);
    }
    
    /**
     * Build JSONB filter string from attribute map.
     * 
     * @param attributes the attribute filters
     * @return JSON string for JSONB query
     */
    private String buildJsonbFilter(Map<String, String> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new InvalidQueryException("Failed to build JSONB filter", e);
        }
    }
    
    /**
     * Validate query inputs to prevent SQL injection and invalid queries.
     * 
     * @param request the chart query request
     * @param seriesSpec the series specification
     */
    private void validateInputs(ChartQueryRequest request, SeriesSpec seriesSpec) {
        // Validate user ID
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new InvalidQueryException("User ID is required");
        }
        
        // Validate metric type (enum validation ensures it's safe)
        if (request.getMetricType() == null) {
            throw new InvalidQueryException("Metric type is required");
        }
        
        // Validate time range
        if (request.getTimeRange() == null) {
            throw new InvalidQueryException("Time range is required");
        }
        
        // Validate metric name (basic validation - no SQL keywords)
        String metricName = seriesSpec.getMetricName();
        if (metricName == null || metricName.trim().isEmpty()) {
            throw new InvalidQueryException("Metric name is required");
        }
        if (containsSqlKeywords(metricName)) {
            throw new InvalidQueryException("Invalid metric name: contains SQL keywords");
        }
        
        // Validate attribute filter keys and values
        if (seriesSpec.hasAttributeFilters()) {
            for (Map.Entry<String, String> entry : seriesSpec.getAttributeFilter().entrySet()) {
                if (containsSqlKeywords(entry.getKey()) || containsSqlKeywords(entry.getValue())) {
                    throw new InvalidQueryException("Invalid attribute filter: contains SQL keywords");
                }
            }
        }
    }
    
    /**
     * Check if a string contains common SQL keywords that could indicate injection attempts.
     * 
     * @param value the string to check
     * @return true if SQL keywords are found
     */
    private boolean containsSqlKeywords(String value) {
        if (value == null) {
            return false;
        }
        String upperValue = value.toUpperCase();
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "EXEC", "EXECUTE", "UNION", "DECLARE", "--", "/*", "*/", ";"
        };
        
        for (String keyword : sqlKeywords) {
            if (upperValue.contains(keyword)) {
                log.warn("Potential SQL injection attempt detected: {}", value);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Container class for SQL query and its parameters.
     */
    public record QueryWithParameters(
            String sql,
            MapSqlParameterSource parameters
    ) {}
}

// Made with Bob
