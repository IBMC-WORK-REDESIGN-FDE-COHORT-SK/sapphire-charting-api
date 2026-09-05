package com.health.charting.service.evaluator;

import com.health.charting.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for managing metric status evaluators.
 * Provides lookup of evaluators by metric type.
 * 
 * This component automatically discovers all MetricStatusEvaluator beans
 * and builds a registry for efficient lookup. If no specific evaluator
 * exists for a metric type, the default evaluator is used.
 */
@Slf4j
@Component
public class MetricStatusEvaluatorRegistry {
    
    private final Map<MetricType, MetricStatusEvaluator> evaluators;
    private final MetricStatusEvaluator defaultEvaluator;
    
    /**
     * Constructor that auto-wires all MetricStatusEvaluator implementations.
     * 
     * @param evaluatorList List of all MetricStatusEvaluator beans
     */
    public MetricStatusEvaluatorRegistry(List<MetricStatusEvaluator> evaluatorList) {
        // Build map of evaluators by metric type (excluding default evaluator)
        this.evaluators = evaluatorList.stream()
                .filter(e -> e.getMetricType() != null)
                .collect(Collectors.toMap(
                        MetricStatusEvaluator::getMetricType,
                        Function.identity()
                ));
        
        // Find default evaluator (the one with null metric type)
        this.defaultEvaluator = evaluatorList.stream()
                .filter(e -> e.getMetricType() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DefaultStatusEvaluator not found"));
        
        log.info("Initialized MetricStatusEvaluatorRegistry with {} evaluators", evaluators.size());
        evaluators.keySet().forEach(type -> 
            log.debug("Registered evaluator for metric type: {}", type)
        );
    }
    
    /**
     * Get the appropriate evaluator for a metric type.
     * Returns default evaluator if no specific evaluator exists.
     * 
     * @param metricType The metric type to get an evaluator for
     * @return The appropriate MetricStatusEvaluator
     */
    public MetricStatusEvaluator getEvaluator(MetricType metricType) {
        MetricStatusEvaluator evaluator = evaluators.getOrDefault(metricType, defaultEvaluator);
        log.debug("Retrieved evaluator for metric type {}: {}", 
                metricType, evaluator.getClass().getSimpleName());
        return evaluator;
    }
}

// Made with Bob