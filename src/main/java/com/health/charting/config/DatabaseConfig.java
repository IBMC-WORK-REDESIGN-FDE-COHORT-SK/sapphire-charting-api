package com.health.charting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Database configuration for the Charting API.
 * Configures JDBC templates and connection pooling.
 */
@Slf4j
@Configuration
public class DatabaseConfig {
    
    /**
     * Create a NamedParameterJdbcTemplate bean for safe parameterized queries.
     * This is the primary way to execute SQL queries in the application.
     * 
     * @param dataSource the configured data source
     * @return configured NamedParameterJdbcTemplate
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        log.info("Configuring NamedParameterJdbcTemplate with DataSource");
        return new NamedParameterJdbcTemplate(dataSource);
    }
}

// Made with Bob
