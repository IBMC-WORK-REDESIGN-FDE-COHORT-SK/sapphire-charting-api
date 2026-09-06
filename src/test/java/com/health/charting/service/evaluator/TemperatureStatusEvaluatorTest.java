package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemperatureStatusEvaluator Unit Tests")
class TemperatureStatusEvaluatorTest {

    private TemperatureStatusEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new TemperatureStatusEvaluator();
    }

    @Test
    @DisplayName("Should return TEMPERATURE metric type")
    void testGetMetricType() {
        assertThat(evaluator.getMetricType()).isEqualTo(MetricType.TEMPERATURE);
    }

    @Test
    @DisplayName("Should evaluate hypothermia for readings below 35.0 °C")
    void testHypothermia() {
        Map<String, Number> values = Map.of("normalized_value_celsius", 34.5);
        Optional<String> status = evaluator.evaluate(values);
        assertThat(status).isPresent().contains("HYPOTHERMIA");
    }

    @Test
    @DisplayName("Should evaluate normal for readings between 35.0 and 37.5 °C")
    void testNormal() {
        Map<String, Number> values = Map.of("normalized_value_celsius", 36.8);
        Optional<String> status = evaluator.evaluate(values);
        assertThat(status).isPresent().contains("NORMAL");
    }

    @Test
    @DisplayName("Should evaluate fever for readings between 37.6 and 39.0 °C")
    void testFever() {
        Map<String, Number> values = Map.of("normalized_value_celsius", 38.2);
        Optional<String> status = evaluator.evaluate(values);
        assertThat(status).isPresent().contains("FEVER");
    }

    @Test
    @DisplayName("Should evaluate high fever for readings above 39.0 °C")
    void testHighFever() {
        Map<String, Number> values = Map.of("normalized_value_celsius", 39.8);
        Optional<String> status = evaluator.evaluate(values);
        assertThat(status).isPresent().contains("HIGH_FEVER");
    }

    @Test
    @DisplayName("Should return empty optional when value is missing")
    void testMissingValue() {
        Map<String, Number> values = new HashMap<>();
        Optional<String> status = evaluator.evaluate(values);
        assertThat(status).isEmpty();
    }
}
