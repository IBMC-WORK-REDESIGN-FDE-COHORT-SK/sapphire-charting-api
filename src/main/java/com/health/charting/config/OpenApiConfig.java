package com.health.charting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${spring.application.name:charting-api}")
    private String applicationName;
    
    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;
    
    @Value("${spring.application.description:TimescaleDB-based charting API}")
    private String applicationDescription;
    
    @Bean
    public OpenAPI customOpenAPI() {
        // Define security scheme for JWT Bearer token
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Bearer token from Keycloak");
        
        // Define security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearer-jwt");
        
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version(applicationVersion)
                        .description(applicationDescription + "\n\n" +
                                "This API provides flexible charting capabilities for health metrics stored in TimescaleDB. " +
                                "It supports multiple metric types, time-based aggregations, and dynamic query generation.\n\n" +
                                "**Authentication**: This API requires JWT Bearer token authentication. " +
                                "Obtain a token from Keycloak and include it in the Authorization header as 'Bearer <token>'.")
                        .contact(new Contact()
                                .name("Health Charting API Team")
                                .email("support@healthcharting.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8089")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.healthcharting.com")
                                .description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}

// Made with Bob
