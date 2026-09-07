package com.health.charting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Enables Spring AOP proxy support for aspect-oriented features such as
 * {@code TemperatureAuditAspect}.
 *
 * <p>Note: {@code @EnableMethodSecurity} is already present on {@link SecurityConfig}
 * and does not conflict with this configuration.
 */
@Configuration
@EnableAspectJAutoProxy
public class AuditConfig {
}

// Made with Bob
