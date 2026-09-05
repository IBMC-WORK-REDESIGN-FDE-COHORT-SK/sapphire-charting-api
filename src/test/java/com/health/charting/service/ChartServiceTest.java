package com.health.charting.service;

import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.request.SeriesSpec;
import com.health.charting.dto.request.TimeRange;
import com.health.charting.dto.response.ChartResponse;
import com.health.charting.dto.response.DataPoint;
import com.health.charting.dto.response.Series;
import com.health.charting.enums.Aggregation;
import com.health.charting.enums.ChartType;
import com.health.charting.enums.MetricType;
import com.health.charting.enums.Resolution;
import com.health.charting.exception.DataNotFoundException;
import com.health.charting.service.impl.ChartQueryServiceImpl;
import com.health.charting.util.SqlQueryBuilder;
import com.health.charting.util.SqlQueryBuilder.QueryWithParameters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

    private static final String USER_ID = "u1";
    private static final String METRIC_NAME = "heartrate";
    private static final String SERIES_NAME = "Heart Rate";
    private static final String DATABASE_QUERY_METRIC = "charting.api.database.queries.total";
    private static final String DATABASE_ERRORS_METRIC = "charting.api.database.errors";

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private SqlQueryBuilder sqlQueryBuilder;

    @Mock
    private Timer databaseQueryTimer;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private ChartQueryServiceImpl chartService;

    @SuppressWarnings("unchecked")
    @Test
    void query_shouldReturnSeriesAndDefaultChartType_whenRequestDoesNotSpecifyChartType() {
        ChartQueryRequest request = buildRequest(null);
        QueryWithParameters query = new QueryWithParameters("SELECT 1", new MapSqlParameterSource());
        List<DataPoint> dataPoints = List.of(new DataPoint(1710000000000L, 80.0));

        when(sqlQueryBuilder.buildSeriesQuery(any(ChartQueryRequest.class), any(SeriesSpec.class))).thenReturn(query);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(databaseQueryTimer.record(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<List<DataPoint>>) invocation.getArgument(0)).get());
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(dataPoints);

        ChartResponse response = chartService.query(request);

        assertEquals(ChartType.LINE, response.getChartType());
        assertEquals(1, response.getSeriesCount());
        assertEquals(1, response.getTotalDataPoints());
        assertEquals(SERIES_NAME, response.getSeries().get(0).getName());
        verify(counter).increment();
        verify(counter).increment(1.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void query_shouldThrowDataNotFoundException_whenNoSeriesReturnsData() {
        ChartQueryRequest request = buildRequest(ChartType.BAR);
        QueryWithParameters query = new QueryWithParameters("SELECT 1", new MapSqlParameterSource());

        when(sqlQueryBuilder.buildSeriesQuery(any(ChartQueryRequest.class), any(SeriesSpec.class))).thenReturn(query);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(databaseQueryTimer.record(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<List<DataPoint>>) invocation.getArgument(0)).get());
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> chartService.query(request));

        assertTrue(exception.getMessage().contains(USER_ID));
        verify(counter).increment(0.0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void query_shouldMapResultSetRowsAndHandleUnexpectedBucketType() throws Exception {
        ChartQueryRequest request = buildRequest(ChartType.BAR);
        QueryWithParameters query = new QueryWithParameters("SELECT 1", new MapSqlParameterSource());

        when(sqlQueryBuilder.buildSeriesQuery(any(ChartQueryRequest.class), any(SeriesSpec.class))).thenReturn(query);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(databaseQueryTimer.record(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<List<DataPoint>>) invocation.getArgument(0)).get());
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<DataPoint> mapper = invocation.getArgument(2);

                    var rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getObject("bucket")).thenReturn(new Timestamp(1710000000000L));
                    when(rs.getDouble("value")).thenReturn(80.5);
                    when(rs.wasNull()).thenReturn(false);
                    DataPoint tsPoint = mapper.mapRow(rs, 0);

                    var rsLong = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rsLong.getObject("bucket")).thenReturn(1710000010000L);
                    when(rsLong.getDouble("value")).thenReturn(81.5);
                    when(rsLong.wasNull()).thenReturn(false);
                    DataPoint longPoint = mapper.mapRow(rsLong, 1);

                    var rsUnexpected = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rsUnexpected.getObject("bucket")).thenReturn("unexpected");
                    when(rsUnexpected.getDouble("value")).thenReturn(0.0);
                    when(rsUnexpected.wasNull()).thenReturn(true);
                    DataPoint fallbackPoint = mapper.mapRow(rsUnexpected, 2);

                    return List.of(tsPoint, longPoint, fallbackPoint);
                });

        ChartResponse response = chartService.query(request);

        assertEquals(ChartType.BAR, response.getChartType());
        assertEquals(3, response.getTotalDataPoints());
        Series series = response.getSeries().get(0);
        assertNotNull(series);
        assertEquals(1710000000000L, series.getPoints().get(0).timestamp());
        assertEquals(1710000010000L, series.getPoints().get(1).timestamp());
        assertEquals(0L, series.getPoints().get(2).timestamp());
        assertEquals(null, series.getPoints().get(2).value());
    }

    @SuppressWarnings("unchecked")
    @Test
    void query_shouldRecordErrorMetricAndRethrow_whenJdbcTemplateFails() {
        ChartQueryRequest request = buildRequest(ChartType.LINE);
        QueryWithParameters query = new QueryWithParameters("SELECT 1", new MapSqlParameterSource());
        RuntimeException dbError = new RuntimeException("db error");

        when(sqlQueryBuilder.buildSeriesQuery(any(ChartQueryRequest.class), any(SeriesSpec.class))).thenReturn(query);
        when(meterRegistry.counter(eq(DATABASE_QUERY_METRIC), any(String[].class))).thenReturn(counter);
        when(meterRegistry.counter(eq(DATABASE_ERRORS_METRIC), any(String[].class))).thenReturn(counter);
        when(databaseQueryTimer.record(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<List<DataPoint>>) invocation.getArgument(0)).get());
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenThrow(dbError);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> chartService.query(request));

        assertEquals("db error", thrown.getMessage());
        verify(meterRegistry).counter(eq(DATABASE_ERRORS_METRIC), any(String[].class));
    }

    private ChartQueryRequest buildRequest(ChartType chartType) {
        SeriesSpec seriesSpec = SeriesSpec.builder()
                .name(SERIES_NAME)
                .metricName(METRIC_NAME)
                .aggregation(Aggregation.AVG)
                .build();

        return ChartQueryRequest.builder()
                .userId(USER_ID)
                .metricType(MetricType.HEARTRATE)
                .timeRange(new TimeRange(1700000000000L, 1700003600000L))
                .resolution(Resolution.RAW)
                .chartType(chartType)
                .series(List.of(seriesSpec))
                .build();
    }
}
