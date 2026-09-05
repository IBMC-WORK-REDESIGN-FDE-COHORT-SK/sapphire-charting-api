package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Status evaluator for blood glucose readings.
 * Based on standard fasting glucose levels (mg/dL).
 * 
 * Status Categories:
 * - HIGH: Glucose > 140 mg/dL (Hyperglycemia)
 * - ELEVATED: 100 < Glucose ≤ 140 mg/dL (Prediabetes range)
 * - NORMAL: 70 ≤ Glucose ≤ 100 mg/dL
 * - LOW: Glucose < 70 mg/dL (Hypoglycemia)
 */
@Slf4j
@Component
public class GlucoseStatusEvaluator implements MetricStatusEvaluator {
    
    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        // Try different possible key names
        Number glucoseValue = values.get("glucose");
        if (glucoseValue == null) {
            glucoseValue = values.get("health_glucose");
        }
        if (glucoseValue == null) {
            glucoseValue = values.get("value");
        }
        
        if (glucoseValue == null) {
            log.debug("Glucose reading missing value. Available keys: {}", values.keySet());
            return Optional.empty();
        }
        
        double glucose = glucoseValue.doubleValue();
        
        if (glucose < 70) {
            return Optional.of("LOW");
        } else if (glucose <= 100) {
            return Optional.of("NORMAL");
        } else if (glucose <= 140) {
            return Optional.of("ELEVATED");
        } else {
            return Optional.of("HIGH");
        }
    }
    
    @Override
    public MetricType getMetricType() {
        return MetricType.GLUCOSE;
    }
}

// Made with Bob