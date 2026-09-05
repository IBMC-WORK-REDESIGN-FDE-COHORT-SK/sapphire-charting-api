package com.health.charting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application class for the Charting API.
 * This Spring Boot application provides REST endpoints for querying
 * health metrics from TimescaleDB and returning formatted chart data.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class ChartingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChartingApiApplication.class, args);
    }
}

// Made with Bob
