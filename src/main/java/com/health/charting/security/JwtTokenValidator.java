package com.health.charting.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for JWT token validation and extraction.
 * Provides helper methods to access JWT claims and user information.
 */
@Slf4j
@Component
public class JwtTokenValidator {
    
    /**
     * Get the current authenticated JWT token.
     * 
     * @return the JWT token or null if not authenticated
     */
    public Jwt getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return (Jwt) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * Get the user ID from the current JWT token.
     * Tries multiple claim names commonly used by Keycloak.
     * 
     * @return the user ID or null if not found
     */
    public String getUserId() {
        Jwt jwt = getCurrentToken();
        if (jwt == null) {
            return null;
        }
        
        // Try different claim names
        String userId = jwt.getClaimAsString("sub");
        if (userId == null) {
            userId = jwt.getClaimAsString("user_id");
        }
        if (userId == null) {
            userId = jwt.getClaimAsString("preferred_username");
        }
        
        return userId;
    }
    
    /**
     * Get the username from the current JWT token.
     * 
     * @return the username or null if not found
     */
    public String getUsername() {
        Jwt jwt = getCurrentToken();
        if (jwt == null) {
            return null;
        }
        
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            username = jwt.getClaimAsString("name");
        }
        if (username == null) {
            username = jwt.getClaimAsString("email");
        }
        
        return username;
    }
    
    /**
     * Get the email from the current JWT token.
     * 
     * @return the email or null if not found
     */
    public String getEmail() {
        Jwt jwt = getCurrentToken();
        return jwt != null ? jwt.getClaimAsString("email") : null;
    }
    
    /**
     * Get all claims from the current JWT token.
     * 
     * @return map of all claims or null if not authenticated
     */
    public Map<String, Object> getAllClaims() {
        Jwt jwt = getCurrentToken();
        return jwt != null ? jwt.getClaims() : null;
    }
    
    /**
     * Check if the current user is authenticated.
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
    
    /**
     * Get a specific claim from the JWT token.
     * 
     * @param claimName the name of the claim
     * @return the claim value or null if not found
     */
    public Object getClaim(String claimName) {
        Jwt jwt = getCurrentToken();
        return jwt != null ? jwt.getClaim(claimName) : null;
    }
    
    /**
     * Log token information for debugging.
     */
    public void logTokenInfo() {
        Jwt jwt = getCurrentToken();
        if (jwt != null) {
            log.debug("JWT Token Info:");
            log.debug("  Subject: {}", jwt.getSubject());
            log.debug("  Issuer: {}", jwt.getIssuer());
            log.debug("  Issued At: {}", jwt.getIssuedAt());
            log.debug("  Expires At: {}", jwt.getExpiresAt());
            log.debug("  Claims: {}", jwt.getClaims());
        } else {
            log.debug("No JWT token found in security context");
        }
    }
}

// Made with Bob