package com.health.charting.service;

import com.health.charting.dto.response.MetricReadingDto;
import com.health.charting.dto.response.MetricReadingsResponse;
import com.health.charting.enums.MetricType;
import com.health.charting.repository.MetricReadingRepository;
import com.health.charting.service.evaluator.MetricStatusEvaluator;
import com.health.charting.service.evaluator.MetricStatusEvaluatorRegistry;
import com.health.charting.util.TimestampAggregator.RawMetricRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricReadingServiceTest {

    private static final String USER_ID = "u1";
    private static final String METRIC_TABLE_NAME = "heartrate";
    private static final String METRIC_ENUM_NAME = "HEARTRATE";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String UNIT_BPM = "bpm";
    private static final String SOURCE_DEVICE = "watch";

    @Mock
    private MetricReadingRepository repository;

    @Mock
    private MetricStatusEvaluatorRegistry evaluatorRegistry;

    @Mock
    private MetricStatusEvaluator evaluator;

    @InjectMocks
    private MetricReadingService metricReadingService;

    @Test
    void fetchReadings_shouldCapPageSizeAndReturnMappedDtos_whenMetricMatchesTableName() {
        long timestampMillis = 1700000000000L;
        List<RawMetricRow> rows = List.of(new RawMetricRow(timestampMillis, METRIC_TABLE_NAME, 72.0, UNIT_BPM, SOURCE_DEVICE));

        when(repository.fetchReadings(MetricType.HEARTRATE, USER_ID, 0, 50, null, null)).thenReturn(rows);
        when(evaluatorRegistry.getEvaluator(MetricType.HEARTRATE)).thenReturn(evaluator);
        when(evaluator.evaluate(Map.of(METRIC_TABLE_NAME, 72.0))).thenReturn(Optional.of(STATUS_NORMAL));

        MetricReadingsResponse response = metricReadingService.fetchReadings(METRIC_TABLE_NAME, USER_ID, 0, 99, null, null);

        assertEquals(METRIC_TABLE_NAME, response.getMetric());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(1, response.getData().size());
        MetricReadingDto dto = response.getData().get(0);
        assertEquals(Instant.ofEpochMilli(timestampMillis), dto.getTimestamp());
        assertEquals(STATUS_NORMAL, dto.getStatus());
        verify(repository).fetchReadings(MetricType.HEARTRATE, USER_ID, 0, 50, null, null);
    }

    @Test
    void fetchReadings_shouldResolveMetricTypeByEnumName_whenTableNameLookupFails() {
        long timestampMillis = 1700001000000L;
        List<RawMetricRow> rows = List.of(new RawMetricRow(timestampMillis, METRIC_TABLE_NAME, 68.0, UNIT_BPM, SOURCE_DEVICE));

        when(repository.fetchReadings(MetricType.HEARTRATE, USER_ID, 1, 10, 100L, 200L)).thenReturn(rows);
        when(evaluatorRegistry.getEvaluator(MetricType.HEARTRATE)).thenReturn(evaluator);
        when(evaluator.evaluate(Map.of(METRIC_TABLE_NAME, 68.0))).thenReturn(Optional.empty());

        MetricReadingsResponse response = metricReadingService.fetchReadings(METRIC_ENUM_NAME, USER_ID, 1, 10, 100L, 200L);

        assertEquals(1, response.getData().size());
        assertEquals(null, response.getData().get(0).getStatus());
        verify(repository).fetchReadings(MetricType.HEARTRATE, USER_ID, 1, 10, 100L, 200L);
    }

    @Test
    void fetchReadings_shouldConvertNanosecondsToMilliseconds_whenTimestampLooksLikeNanoseconds() {
        long timestampNanos = 1700000000000000000L;
        long expectedMillis = 1700000000000L;
        List<RawMetricRow> rows = List.of(new RawMetricRow(timestampNanos, METRIC_TABLE_NAME, 75.0, UNIT_BPM, SOURCE_DEVICE));

        when(repository.fetchReadings(MetricType.HEARTRATE, USER_ID, 0, 5, null, null)).thenReturn(rows);
        when(evaluatorRegistry.getEvaluator(MetricType.HEARTRATE)).thenReturn(evaluator);
        when(evaluator.evaluate(Map.of(METRIC_TABLE_NAME, 75.0))).thenReturn(Optional.of(STATUS_NORMAL));

        MetricReadingsResponse response = metricReadingService.fetchReadings(METRIC_TABLE_NAME, USER_ID, 0, 5, null, null);

        assertEquals(Instant.ofEpochMilli(expectedMillis), response.getData().get(0).getTimestamp());
    }

    @Test
    void fetchReadings_shouldThrowIllegalArgumentException_whenMetricNameIsInvalid() {
        String invalidMetric = "invalid-metric";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> metricReadingService.fetchReadings(invalidMetric, USER_ID, 0, 10, null, null));

        assertTrue(exception.getMessage().contains(invalidMetric));
    }

    @Test
    void calculatePageInfo_shouldReturnPaginationValues_whenReadingsExist() {
        when(repository.countReadings(MetricType.HEARTRATE, USER_ID, null, null)).thenReturn(101L);

        MetricReadingService.PageInfo pageInfo = metricReadingService.calculatePageInfo(METRIC_TABLE_NAME, USER_ID, 1, 50, null, null);

        assertEquals(1, pageInfo.pageNumber);
        assertEquals(50, pageInfo.pageSize);
        assertEquals(101L, pageInfo.totalElements);
        assertEquals(3, pageInfo.totalPages);
        assertTrue(pageInfo.hasNext);
        assertTrue(pageInfo.hasPrevious);
    }

    @Test
    void calculatePageInfo_shouldHandleZeroResults_whenNoReadingsExist() {
        when(repository.countReadings(MetricType.HEARTRATE, USER_ID, 10L, 20L)).thenReturn(0L);

        MetricReadingService.PageInfo pageInfo = metricReadingService.calculatePageInfo(METRIC_TABLE_NAME, USER_ID, 0, 25, 10L, 20L);

        assertEquals(0L, pageInfo.totalElements);
        assertEquals(0, pageInfo.totalPages);
        assertFalse(pageInfo.hasNext);
        assertFalse(pageInfo.hasPrevious);
    }
}
