package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Status evaluator for heart rate readings.
 * Based on standard resting heart rate ranges for adults.
 * 
 * Status Categories:
 * - TACHYCARDIA: Heart rate > 100 bpm (Elevated heart rate)
 * - NORMAL: 60 ≤ Heart rate ≤ 100 bpm
 * - BRADYCARDIA: Heart rate < 60 bpm (Low heart rate)
 */
@Slf4j
@Component
public class HeartRateStatusEvaluator implements MetricStatusEvaluator {
    
    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        // Try different possible key names
        Number hrValue = values.get("heartrate");
        if (hrValue == null) {
            hrValue = values.get("health_heartrate");
        }
        if (hrValue == null) {
            hrValue = values.get("value");
        }
        
        if (hrValue == null) {
            log.debug("Heart rate reading missing value. Available keys: {}", values.keySet());
            return Optional.empty();
        }
        
        int heartRate = hrValue.intValue();
        
        if (heartRate < 60) {
            return Optional.of("BRADYCARDIA");
        } else if (heartRate <= 100) {
            return Optional.of("NORMAL");
        } else {
            return Optional.of("TACHYCARDIA");
        }
    }
    
    @Override
    public MetricType getMetricType() {
        return MetricType.HEARTRATE;
    }
}

// Made with Bob