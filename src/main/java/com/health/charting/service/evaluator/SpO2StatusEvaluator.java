package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Status evaluator for SpO2 (blood oxygen saturation) readings.
 * Based on standard oxygen saturation levels.
 * 
 * Status Categories:
 * - NORMAL: SpO2 ≥ 95% (Normal oxygen saturation)
 * - LOW: 90% ≤ SpO2 < 95% (Mild hypoxemia)
 * - CRITICAL: SpO2 < 90% (Severe hypoxemia - requires immediate attention)
 */
@Slf4j
@Component
public class SpO2StatusEvaluator implements MetricStatusEvaluator {
    
    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        // Try different possible key names
        Number spo2Value = values.get("spo2");
        if (spo2Value == null) {
            spo2Value = values.get("health_spo2");
        }
        if (spo2Value == null) {
            spo2Value = values.get("value");
        }
        
        if (spo2Value == null) {
            log.debug("SpO2 reading missing value. Available keys: {}", values.keySet());
            return Optional.empty();
        }
        
        double spo2 = spo2Value.doubleValue();
        
        if (spo2 < 90) {
            return Optional.of("CRITICAL");
        } else if (spo2 < 95) {
            return Optional.of("LOW");
        } else {
            return Optional.of("NORMAL");
        }
    }
    
    @Override
    public MetricType getMetricType() {
        return MetricType.SPO2;
    }
}

// Made with Bob