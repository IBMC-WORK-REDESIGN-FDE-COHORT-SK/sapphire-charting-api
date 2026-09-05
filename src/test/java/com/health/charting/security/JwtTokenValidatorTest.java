package com.health.charting.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenValidatorTest {

    private static final String TOKEN_VALUE = "token";
    private static final String HEADER_ALG = "alg";
    private static final String HEADER_NONE = "none";
    private static final String CLAIM_SUB = "sub";
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_EMAIL = "email";
    private static final String USER_ID = "u1";
    private static final String USERNAME = "alice";
    private static final String FULL_NAME = "Alice Doe";
    private static final String EMAIL = "alice@example.com";

    private final JwtTokenValidator jwtTokenValidator = new JwtTokenValidator();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentToken_shouldReturnJwt_whenAuthenticationPrincipalIsJwt() {
        Jwt jwt = buildJwt(Map.of(CLAIM_SUB, USER_ID));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Jwt result = jwtTokenValidator.getCurrentToken();

        assertNotNull(result);
        assertEquals(USER_ID, result.getClaimAsString(CLAIM_SUB));
    }

    @Test
    void getUserId_shouldUseFallbackClaims_whenSubIsMissing() {
        Jwt jwt = buildJwt(Map.of(CLAIM_USER_ID, USER_ID));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        String result = jwtTokenValidator.getUserId();

        assertEquals(USER_ID, result);
    }

    @Test
    void getUsername_shouldFallbackFromPreferredUsernameToNameToEmail() {
        Jwt jwtFromName = buildJwt(Map.of(CLAIM_NAME, FULL_NAME));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwtFromName, null));

        String fromName = jwtTokenValidator.getUsername();

        assertEquals(FULL_NAME, fromName);

        Jwt jwtFromEmail = buildJwt(Map.of(CLAIM_EMAIL, EMAIL));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwtFromEmail, null));

        String fromEmail = jwtTokenValidator.getUsername();

        assertEquals(EMAIL, fromEmail);
    }

    @Test
    void accessors_shouldReturnNull_whenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();

        String userId = jwtTokenValidator.getUserId();
        String username = jwtTokenValidator.getUsername();
        String email = jwtTokenValidator.getEmail();
        Object claim = jwtTokenValidator.getClaim(CLAIM_SUB);
        Map<String, Object> claims = jwtTokenValidator.getAllClaims();

        assertNull(userId);
        assertNull(username);
        assertNull(email);
        assertNull(claim);
        assertNull(claims);
    }

    @Test
    void isAuthenticated_shouldReturnTrueOnlyForAuthenticatedPrincipal() {
        TestingAuthenticationToken unauthenticated = new TestingAuthenticationToken("principal", null);
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        boolean unauthenticatedResult = jwtTokenValidator.isAuthenticated();

        assertFalse(unauthenticatedResult);

        Jwt jwt = buildJwt(Map.of(CLAIM_PREFERRED_USERNAME, USERNAME));
        TestingAuthenticationToken authenticated = new TestingAuthenticationToken(jwt, null);
        authenticated.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authenticated);

        boolean authenticatedResult = jwtTokenValidator.isAuthenticated();

        assertTrue(authenticatedResult);
    }

    @Test
    void claimAndEmailAccessors_shouldReturnValues_whenClaimsExist() {
        Jwt jwt = buildJwt(Map.of(CLAIM_SUB, USER_ID, CLAIM_EMAIL, EMAIL));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        Object subClaim = jwtTokenValidator.getClaim(CLAIM_SUB);
        String email = jwtTokenValidator.getEmail();

        assertEquals(USER_ID, subClaim);
        assertEquals(EMAIL, email);
    }

    @Test
    void logTokenInfo_shouldNotThrow_whenTokenMissingOrPresent() {
        SecurityContextHolder.clearContext();

        jwtTokenValidator.logTokenInfo();

        Jwt jwt = buildJwt(Map.of(CLAIM_SUB, USER_ID));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        jwtTokenValidator.logTokenInfo();

        assertTrue(true);
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
