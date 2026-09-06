package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Status evaluator for body temperature readings.
 * Evaluates temperature in Celsius based on clinical thresholds.
 * 
 * Status Categories:
 * - HYPOTHERMIA: Temperature < 35.0 °C (< 95.0 °F)
 * - NORMAL: 35.0 °C ≤ Temperature ≤ 37.5 °C (95.0 °F - 99.5 °F)
 * - FEVER: 37.5 °C < Temperature ≤ 39.0 °C (99.5 °F - 102.2 °F)
 * - HIGH_FEVER: Temperature > 39.0 °C (> 102.2 °F)
 */
@Slf4j
@Component
public class TemperatureStatusEvaluator implements MetricStatusEvaluator {

    private static final double HYPOTHERMIA_THRESHOLD = 35.0;
    private static final double NORMAL_UPPER_THRESHOLD = 37.5;
    private static final double FEVER_UPPER_THRESHOLD = 39.0;

    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        Number tempValue = values.get("normalized_value_celsius");
        if (tempValue == null) {
            tempValue = values.get("temperature");
        }
        if (tempValue == null) {
            tempValue = values.get("value");
        }

        if (tempValue == null) {
            log.debug("Temperature reading missing value. Available keys: {}", values.keySet());
            return Optional.empty();
        }

        double tempCelsius = tempValue.doubleValue();

        if (tempCelsius < HYPOTHERMIA_THRESHOLD) {
            return Optional.of("HYPOTHERMIA");
        } else if (tempCelsius <= NORMAL_UPPER_THRESHOLD) {
            return Optional.of("NORMAL");
        } else if (tempCelsius <= FEVER_UPPER_THRESHOLD) {
            return Optional.of("FEVER");
        } else {
            return Optional.of("HIGH_FEVER");
        }
    }

    @Override
    public MetricType getMetricType() {
        return MetricType.TEMPERATURE;
    }
}
