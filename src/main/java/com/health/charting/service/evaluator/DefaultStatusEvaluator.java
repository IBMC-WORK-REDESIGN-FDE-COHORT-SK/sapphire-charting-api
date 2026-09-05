package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Default status evaluator for metrics without specific status rules.
 * Returns empty Optional, indicating no status can be computed.
 * 
 * This evaluator is used as a fallback for metric types that don't have
 * specific health status criteria defined (e.g., activity, sleep, workout).
 */
@Slf4j
@Component
public class DefaultStatusEvaluator implements MetricStatusEvaluator {
    
    @Override
    public Optional<String> evaluate(Map<String, Number> values) {
        log.debug("No specific status evaluator available for this metric, returning empty status");
        return Optional.empty();
    }
    
    @Override
    public MetricType getMetricType() {
        // This is a fallback evaluator, not tied to a specific metric type
        return null;
    }
}

// Made with Bob