package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for evaluating health status based on metric values.
 * Each metric type can have its own implementation with specific rules.
 * 
 * Implementations should be registered as Spring components and will be
 * automatically discovered by the MetricStatusEvaluatorRegistry.
 */
public interface MetricStatusEvaluator {
    
    /**
     * Evaluate the status of a metric reading based on its values.
     * 
     * @param values Map of metric component names to their numeric values
     *               (e.g., {"systolic": 120, "diastolic": 80} for blood pressure)
     * @return Optional status string, empty if status cannot be determined
     */
    Optional<String> evaluate(Map<String, Number> values);
    
    /**
     * Get the metric type this evaluator handles.
     * 
     * @return The MetricType this evaluator is designed for, or null for default evaluator
     */
    MetricType getMetricType();
}

// Made with Bob