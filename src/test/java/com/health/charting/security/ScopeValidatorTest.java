package com.health.charting.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeValidatorTest {

    private static final String TOKEN_VALUE = "token";
    private static final String HEADER_ALG = "alg";
    private static final String HEADER_NONE = "none";
    private static final String CLAIM_SCOPE = "scope";
    private static final String ERROR_INVALID_TOKEN = "invalid_token";
    private static final String ERROR_INSUFFICIENT_SCOPE = "insufficient_scope";
    private static final String REQUIRED_READ = "read";
    private static final String REQUIRED_WRITE = "write";

    @Test
    void validate_shouldSucceed_whenTokenContainsAllRequiredScopes() {
        ScopeValidator scopeValidator = new ScopeValidator();
        scopeValidator.setRequiredScopes(List.of(REQUIRED_READ, REQUIRED_WRITE));
        Jwt jwt = buildJwt(Map.of(CLAIM_SCOPE, "read write admin"));

        OAuth2TokenValidatorResult result = scopeValidator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    void validate_shouldFailWithInvalidToken_whenScopeClaimIsMissing() {
        ScopeValidator scopeValidator = new ScopeValidator();
        scopeValidator.setRequiredScopes(List.of(REQUIRED_READ));
        Jwt jwt = buildJwt(Map.of("sub", "u1"));

        OAuth2TokenValidatorResult result = scopeValidator.validate(jwt);

        assertTrue(result.hasErrors());
        OAuth2Error error = result.getErrors().iterator().next();
        assertEquals(ERROR_INVALID_TOKEN, error.getErrorCode());
    }

    @Test
    void validate_shouldFailWithInsufficientScope_whenAnyRequiredScopeIsMissing() {
        ScopeValidator scopeValidator = new ScopeValidator();
        scopeValidator.setRequiredScopes(List.of(REQUIRED_READ, REQUIRED_WRITE));
        Jwt jwt = buildJwt(Map.of(CLAIM_SCOPE, "read"));

        OAuth2TokenValidatorResult result = scopeValidator.validate(jwt);

        assertTrue(result.hasErrors());
        OAuth2Error error = result.getErrors().iterator().next();
        assertEquals(ERROR_INSUFFICIENT_SCOPE, error.getErrorCode());
        assertTrue(error.getDescription().contains(REQUIRED_WRITE));
    }

    @Test
    void validate_shouldSucceed_whenNoRequiredScopesConfiguredAndScopeClaimPresent() {
        ScopeValidator scopeValidator = new ScopeValidator();
        scopeValidator.setRequiredScopes(List.of());
        Jwt jwt = buildJwt(Map.of(CLAIM_SCOPE, "any"));

        OAuth2TokenValidatorResult result = scopeValidator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return new Jwt(
                TOKEN_VALUE,
                Instant.now(),
                Instant.now().plusSeconds(600),
                Map.of(HEADER_ALG, HEADER_NONE),
                claims
        );
    }
}
