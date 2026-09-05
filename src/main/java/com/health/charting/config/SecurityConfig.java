package com.health.charting.config;

import com.health.charting.security.JwtAuthenticationConverter;
import com.health.charting.security.JwtAuthenticationFailureHandler;
import com.health.charting.security.ScopeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for JWT token validation with Keycloak.
 * Configures OAuth2 resource server to validate JWT tokens using Keycloak's JWK endpoint.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ScopeValidator scopeValidator;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    
    /**
     * Configure security filter chain.
     * - Allows public access to Swagger/OpenAPI endpoints
     * - Requires authentication for all API endpoints
     * - Configures stateless session management
     * - Enables JWT token validation
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain with JWT validation");
        
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationFailureHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Allow public access to Swagger/OpenAPI documentation and actuator endpoints
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/metrics/**",
                    "/actuator/prometheus"
                ).permitAll()
                // Require authentication for all API endpoints
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(jwtAuthenticationFailureHandler)
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            );
        
        return http.build();
    }
    
    /**
     * Configure JWT decoder to validate tokens using Keycloak's JWK endpoint.
     * The JWK endpoint provides public keys to verify JWT signatures.
     * Also adds custom scope validation.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with JWK Set URI: {}", jwkSetUri);
        log.info("Configuring JWT decoder with Issuer URI: {}", issuerUri);
        
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        
        // Create validator chain: issuer validation + scope validation
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withScopeAndIssuer = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                scopeValidator
        );
        
        jwtDecoder.setJwtValidator(withScopeAndIssuer);
        
        log.info("JWT decoder configured with issuer and scope validation");
        return jwtDecoder;
    }
}

// Made with Bob