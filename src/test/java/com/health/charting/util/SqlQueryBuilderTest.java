package com.health.charting.util;

import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.request.SeriesSpec;
import com.health.charting.dto.request.TimeRange;
import com.health.charting.enums.Aggregation;
import com.health.charting.enums.MetricType;
import com.health.charting.enums.Resolution;
import com.health.charting.exception.InvalidQueryException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlQueryBuilderTest {

    private static final String SCHEMA = "health";
    private static final String USER_ID = "u1";
    private static final String METRIC_HEARTRATE = "heartrate";
    private static final String METRIC_WITH_SQL = "select * from users";
    private static final String DEVICE_TYPE_KEY = "device_type";
    private static final String DEVICE_TYPE_VALUE = "watch";

    @Test
    void buildSeriesQuery_shouldGenerateRawQueryWithoutGroupBy_whenResolutionIsRaw() {
        SqlQueryBuilder sqlQueryBuilder = new SqlQueryBuilder();
        ReflectionTestUtils.setField(sqlQueryBuilder, "databaseSchema", SCHEMA);
        ChartQueryRequest request = ChartQueryRequest.builder()
                .userId(USER_ID)
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.RAW)
                .aggregation(Aggregation.AVG)
                .series(java.util.List.of())
                .build();
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name("Heart Rate")
                .metricName(METRIC_HEARTRATE)
                .build();

        SqlQueryBuilder.QueryWithParameters query = sqlQueryBuilder.buildSeriesQuery(request, seriesSpec);

        assertTrue(query.sql().contains("SELECT time AS bucket"));
        assertTrue(query.sql().contains("FROM health.heartrate"));
        assertFalse(query.sql().contains("GROUP BY bucket"));
        assertTrue(query.sql().contains("ORDER BY bucket"));
        MapSqlParameterSource params = query.parameters();
        assertEquals(USER_ID, params.getValue("userId"));
        assertEquals(METRIC_HEARTRATE, params.getValue("metricName"));
    }

    @Test
    void buildSeriesQuery_shouldGenerateBucketedQueryAndJsonbFilter_whenFiltersExist() {
        SqlQueryBuilder sqlQueryBuilder = new SqlQueryBuilder();
        ReflectionTestUtils.setField(sqlQueryBuilder, "databaseSchema", SCHEMA);
        ChartQueryRequest request = ChartQueryRequest.builder()
                .userId(USER_ID)
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.FIVE_MIN)
                .aggregation(Aggregation.MAX)
                .series(java.util.List.of())
                .build();
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name("Heart Rate")
                .metricName(METRIC_HEARTRATE)
                .attributeFilter(Map.of(DEVICE_TYPE_KEY, DEVICE_TYPE_VALUE))
                .build();

        SqlQueryBuilder.QueryWithParameters query = sqlQueryBuilder.buildSeriesQuery(request, seriesSpec);

        assertTrue(query.sql().contains("time_bucket('5 minutes'"));
        assertTrue(query.sql().contains("GROUP BY bucket"));
        assertTrue(query.sql().contains("attributes @> :attributeFilter::jsonb"));
        assertEquals("{\"device_type\":\"watch\"}", query.parameters().getValue("attributeFilter"));
    }

    @Test
    void buildSeriesQuery_shouldThrowInvalidQueryException_whenUserIdIsBlank() {
        SqlQueryBuilder sqlQueryBuilder = new SqlQueryBuilder();
        ReflectionTestUtils.setField(sqlQueryBuilder, "databaseSchema", SCHEMA);
        ChartQueryRequest request = ChartQueryRequest.builder()
                .userId(" ")
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.RAW)
                .series(java.util.List.of())
                .build();
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name("Heart Rate")
                .metricName(METRIC_HEARTRATE)
                .build();

        InvalidQueryException exception = assertThrows(InvalidQueryException.class,
                () -> sqlQueryBuilder.buildSeriesQuery(request, seriesSpec));

        assertEquals("User ID is required", exception.getMessage());
    }

    @Test
    void buildSeriesQuery_shouldThrowInvalidQueryException_whenMetricNameContainsSqlKeyword() {
        SqlQueryBuilder sqlQueryBuilder = new SqlQueryBuilder();
        ReflectionTestUtils.setField(sqlQueryBuilder, "databaseSchema", SCHEMA);
        ChartQueryRequest request = ChartQueryRequest.builder()
                .userId(USER_ID)
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.RAW)
                .series(java.util.List.of())
                .build();
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name("Heart Rate")
                .metricName(METRIC_WITH_SQL)
                .build();

        InvalidQueryException exception = assertThrows(InvalidQueryException.class,
                () -> sqlQueryBuilder.buildSeriesQuery(request, seriesSpec));

        assertEquals("Invalid metric name: contains SQL keywords", exception.getMessage());
    }

    @Test
    void buildSeriesQuery_shouldThrowInvalidQueryException_whenAttributeContainsSqlKeyword() {
        SqlQueryBuilder sqlQueryBuilder = new SqlQueryBuilder();
        ReflectionTestUtils.setField(sqlQueryBuilder, "databaseSchema", SCHEMA);
        ChartQueryRequest request = ChartQueryRequest.builder()
                .userId(USER_ID)
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.RAW)
                .series(java.util.List.of())
                .build();
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name("Heart Rate")
                .metricName(METRIC_HEARTRATE)
                .attributeFilter(Map.of("attr", "drop table x"))
                .build();

        InvalidQueryException exception = assertThrows(InvalidQueryException.class,
                () -> sqlQueryBuilder.buildSeriesQuery(request, seriesSpec));

        assertEquals("Invalid attribute filter: contains SQL keywords", exception.getMessage());
    }
}
