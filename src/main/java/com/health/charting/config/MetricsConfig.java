package com.health.charting.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for custom application metrics using Micrometer.
 * Provides beans for tracking API performance, errors, and business metrics.
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for total chart query requests.
     */
    @Bean
    public Counter chartQueryRequestCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.requests.total")
                .description("Total number of chart query requests")
                .tag("api", "charting")
                .register(registry);
    }

    /**
     * Counter for successful chart queries.
     */
    @Bean
    public Counter chartQuerySuccessCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.requests.success")
                .description("Number of successful chart query requests")
                .tag("api", "charting")
                .register(registry);
    }

    /**
     * Counter for failed chart queries.
     */
    @Bean
    public Counter chartQueryErrorCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.requests.error")
                .description("Number of failed chart query requests")
                .tag("api", "charting")
                .register(registry);
    }

    /**
     * Timer for chart query execution time.
     */
    @Bean
    public Timer chartQueryTimer(MeterRegistry registry) {
        return Timer.builder("charting.api.query.duration")
                .description("Time taken to execute chart queries")
                .tag("api", "charting")
                .register(registry);
    }

    /**
     * Timer for database query execution time.
     */
    @Bean
    public Timer databaseQueryTimer(MeterRegistry registry) {
        return Timer.builder("charting.api.database.query.duration")
                .description("Time taken to execute database queries")
                .tag("component", "database")
                .register(registry);
    }

    /**
     * Counter for data points returned.
     */
    @Bean
    public Counter dataPointsCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.datapoints.total")
                .description("Total number of data points returned")
                .tag("api", "charting")
                .register(registry);
    }

    /**
     * Counter for validation errors.
     */
    @Bean
    public Counter validationErrorCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.validation.errors")
                .description("Number of validation errors")
                .tag("type", "validation")
                .register(registry);
    }

    /**
     * Counter for authentication failures.
     */
    @Bean
    public Counter authenticationFailureCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.authentication.failures")
                .description("Number of authentication failures")
                .tag("type", "security")
                .register(registry);
    }

    /**
     * Counter for authorization failures.
     */
    @Bean
    public Counter authorizationFailureCounter(MeterRegistry registry) {
        return Counter.builder("charting.api.authorization.failures")
                .description("Number of authorization failures")
                .tag("type", "security")
                .register(registry);
    }
}

// Made with Bob