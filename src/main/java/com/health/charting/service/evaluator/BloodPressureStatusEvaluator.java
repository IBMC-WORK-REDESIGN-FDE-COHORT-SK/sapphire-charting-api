package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Status evaluator for blood pressure readings.
 * Based on American Heart Association guidelines.
 * 
 * Status Categories:
 * - CRISIS: Systolic ≥ 180 OR Diastolic ≥ 120 (Hypertensive Crisis)
 * - STAGE_2: Systolic ≥ 140 OR Diastolic ≥ 90 (Stage 2 Hypertension)
 * - STAGE_1: Systolic ≥ 130 OR Diastolic ≥ 80 (Stage 1 Hypertension)
 * - ELEVATED: Systolic ≥ 120 (Elevated Blood Pressure)
 * - NORMAL: Systolic < 120 AND Diastolic < 80
 */
@Slf4j
@Component
public class BloodPressureStatusEvaluator implements MetricStatusEvaluator {
    
    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        // Try different possible key names for systolic and diastolic
        Number systolicValue = values.get("systolic");
        if (systolicValue == null) {
            systolicValue = values.get("health_bloodpressure_systolic");
        }
        
        Number diastolicValue = values.get("diastolic");
        if (diastolicValue == null) {
            diastolicValue = values.get("health_bloodpressure_diastolic");
        }
        
        if (systolicValue == null || diastolicValue == null) {
            log.debug("Blood pressure reading missing systolic or diastolic values. Available keys: {}", values.keySet());
            return Optional.empty();
        }
        
        int systolic = systolicValue.intValue();
        int diastolic = diastolicValue.intValue();
        
        // Hypertensive Crisis - requires immediate medical attention
        if (systolic >= 180 || diastolic >= 120) {
            return Optional.of("CRISIS");
        }
        
        // Stage 2 Hypertension
        if (systolic >= 140 || diastolic >= 90) {
            return Optional.of("STAGE_2");
        }
        
        // Stage 1 Hypertension
        if (systolic >= 130 || diastolic >= 80) {
            return Optional.of("STAGE_1");
        }
        
        // Elevated Blood Pressure
        if (systolic >= 120) {
            return Optional.of("ELEVATED");
        }
        
        // Normal Blood Pressure
        return Optional.of("NORMAL");
    }
    
    @Override
    public MetricType getMetricType() {
        return MetricType.BLOODPRESSURE;
    }
}

// Made with Bob