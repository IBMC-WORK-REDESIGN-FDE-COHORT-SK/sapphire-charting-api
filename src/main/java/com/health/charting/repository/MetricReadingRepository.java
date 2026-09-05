package com.health.charting.repository;

import com.health.charting.enums.MetricType;
import com.health.charting.util.TimestampAggregator.RawMetricRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for fetching metric readings from TimescaleDB.
 * Uses efficient descending index scans for optimal query performance.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MetricReadingRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    /**
     * Fetch metric readings with efficient descending index scan.
     * Returns raw rows that need to be aggregated by timestamp.
     * 
     * @param metricType The type of metric to query
     * @param userId User identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @param fromTime Optional start time filter (epoch milliseconds)
     * @param toTime Optional end time filter (epoch milliseconds)
     * @return List of raw metric rows
     */
    public List<RawMetricRow> fetchReadings(
            MetricType metricType,
            String userId,
            int page,
            int size,
            Long fromTime,
            Long toTime) {
        
        String tableName = metricType.getTableName();
        int offset = page * size;
        
        // Build SQL query with efficient descending index scan
        // Build dynamic WHERE clause based on optional parameters
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT time, metric_name, metric_value, unit, device_id ");
        sqlBuilder.append("FROM ").append(tableName).append(" ");
        sqlBuilder.append("WHERE user_id = :userId ");
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("size", size)
                .addValue("offset", offset);
        
        // Add optional time filters only if provided
        if (fromTime != null) {
            sqlBuilder.append("AND time >= :fromTime ");
            params.addValue("fromTime", fromTime);
        }
        
        if (toTime != null) {
            sqlBuilder.append("AND time <= :toTime ");
            params.addValue("toTime", toTime);
        }
        
        sqlBuilder.append("ORDER BY time DESC ");
        sqlBuilder.append("LIMIT :size OFFSET :offset");
        
        String sql = sqlBuilder.toString();
        
        log.debug("Executing query on table {} for user {} (page={}, size={})", 
                tableName, userId, page, size);
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> 
            new RawMetricRow(
                rs.getLong("time"),
                rs.getString("metric_name"),
                rs.getDouble("metric_value"),
                rs.getString("unit"),
                rs.getString("device_id")
            )
        );
    }
    
    /**
     * Count total readings for pagination metadata.
     * 
     * @param metricType The type of metric to query
     * @param userId User identifier
     * @param fromTime Optional start time filter (epoch milliseconds)
     * @param toTime Optional end time filter (epoch milliseconds)
     * @return Total count of readings matching the criteria
     */
    public long countReadings(
            MetricType metricType,
            String userId,
            Long fromTime,
            Long toTime) {
        
        String tableName = metricType.getTableName();
        
        // Count distinct timestamps (not individual rows)
        // Build dynamic WHERE clause based on optional parameters
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(DISTINCT time) ");
        sqlBuilder.append("FROM ").append(tableName).append(" ");
        sqlBuilder.append("WHERE user_id = :userId ");
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId);
        
        // Add optional time filters only if provided
        if (fromTime != null) {
            sqlBuilder.append("AND time >= :fromTime ");
            params.addValue("fromTime", fromTime);
        }
        
        if (toTime != null) {
            sqlBuilder.append("AND time <= :toTime ");
            params.addValue("toTime", toTime);
        }
        
        String sql = sqlBuilder.toString();
        
        log.debug("Counting readings on table {} for user {}", tableName, userId);
        
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}

// Made with Bob