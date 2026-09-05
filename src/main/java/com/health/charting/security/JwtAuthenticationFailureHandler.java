package com.health.charting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.health.charting.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Custom authentication failure handler for JWT validation errors.
 * Formats OAuth2 authentication errors (including scope validation failures) 
 * into standardized JSON error responses.
 */
@Slf4j
@Component
public class JwtAuthenticationFailureHandler implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    public JwtAuthenticationFailureHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("JWT authentication failed: {} - {}", authException.getMessage(), request.getRequestURI());
        
        String errorMessage = "Authentication failed";
        String errorDetail = authException.getMessage();
        
        // Extract detailed error information from OAuth2 exceptions
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) authException;
            OAuth2Error error = oauth2Exception.getError();
            
            if (error != null) {
                String errorCode = error.getErrorCode();
                String description = error.getDescription();
                
                log.debug("OAuth2 Error Code: {}, Description: {}", errorCode, description);
                
                // Customize message based on error code
                if ("insufficient_scope".equals(errorCode)) {
                    errorMessage = "Insufficient Scope";
                    errorDetail = description != null ? description : "Token does not have required scopes";
                } else if ("invalid_token".equals(errorCode)) {
                    errorMessage = "Invalid Token";
                    errorDetail = description != null ? description : "Token validation failed";
                } else if (error instanceof BearerTokenError) {
                    BearerTokenError bearerError = (BearerTokenError) error;
                    errorMessage = bearerError.getErrorCode();
                    errorDetail = bearerError.getDescription();
                }
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(errorMessage)
                .message(errorDetail)
                .path(request.getRequestURI())
                .build();
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setContentLength(jsonResponse.getBytes("UTF-8").length);
        
        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
        
        log.info("Sent error response: status={}, error={}, message={}",
                HttpStatus.UNAUTHORIZED.value(), errorMessage, errorDetail);
        log.debug("Full JSON response: {}", jsonResponse);
    }
}

// Made with Bob