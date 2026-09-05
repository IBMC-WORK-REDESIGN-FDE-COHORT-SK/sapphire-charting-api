package com.health.charting.controller;

import com.health.charting.security.JwtTokenValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for JWT authentication.
 * Provides endpoints to verify JWT token validation is working correctly.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Test", description = "Endpoints for testing JWT authentication")
public class AuthTestController {
    
    private final JwtTokenValidator jwtTokenValidator;
    
    /**
     * Test endpoint to verify JWT authentication.
     * Returns information about the authenticated user.
     */
    @GetMapping("/test")
    @Operation(
            summary = "Test JWT authentication",
            description = "Verify that JWT token validation is working and return user information",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<Map<String, Object>> testAuth() {
        log.info("Testing JWT authentication");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", jwtTokenValidator.isAuthenticated());
        response.put("userId", jwtTokenValidator.getUserId());
        response.put("username", jwtTokenValidator.getUsername());
        response.put("email", jwtTokenValidator.getEmail());
        response.put("principal", authentication.getPrincipal().toString());
        response.put("authorities", authentication.getAuthorities());
        response.put("allClaims", jwtTokenValidator.getAllClaims());
        
        // Log token info for debugging
        jwtTokenValidator.logTokenInfo();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user information.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Get information about the currently authenticated user",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        log.info("Getting current user information");
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", jwtTokenValidator.getUserId());
        response.put("username", jwtTokenValidator.getUsername());
        response.put("email", jwtTokenValidator.getEmail());
        
        return ResponseEntity.ok(response);
    }
}

// Made with Bob