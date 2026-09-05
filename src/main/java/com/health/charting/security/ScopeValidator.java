package com.health.charting.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom validator to check if JWT token contains required scopes.
 * Required scopes are configured in application.yml under app.security.required-scopes.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "app.security")
public class ScopeValidator implements OAuth2TokenValidator<Jwt> {
    
    private static final String SCOPE_CLAIM = "scope";
    
    private List<String> requiredScopes = new ArrayList<>();
    
    // Getter and setter for Spring Boot configuration properties
    public List<String> getRequiredScopes() {
        return requiredScopes;
    }
    
    public void setRequiredScopes(List<String> requiredScopes) {
        this.requiredScopes = requiredScopes;
    }
    
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        log.debug("Validating JWT scopes");
        
        // Get scope claim from JWT
        String scopeClaim = jwt.getClaimAsString(SCOPE_CLAIM);
        
        if (scopeClaim == null || scopeClaim.trim().isEmpty()) {
            log.warn("JWT token missing scope claim");
            OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Token is missing required 'scope' claim",
                null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
        
        // Parse scopes (space-separated string)
        List<String> tokenScopes = Arrays.asList(scopeClaim.split("\\s+"));
        log.debug("Token scopes: {}", tokenScopes);
        
        // Check if all required scopes are present
        for (String requiredScope : requiredScopes) {
            if (!tokenScopes.contains(requiredScope)) {
                log.warn("JWT token missing required scope: {}. Required scopes: {}, Token scopes: {}",
                        requiredScope, requiredScopes, tokenScopes);
                OAuth2Error error = new OAuth2Error(
                    "insufficient_scope",
                    String.format("Token is missing required scope: %s. Required scopes: %s",
                            requiredScope, requiredScopes),
                    null
                );
                return OAuth2TokenValidatorResult.failure(error);
            }
        }
        
        log.debug("JWT scope validation successful");
        return OAuth2TokenValidatorResult.success();
    }
}

// Made with Bob