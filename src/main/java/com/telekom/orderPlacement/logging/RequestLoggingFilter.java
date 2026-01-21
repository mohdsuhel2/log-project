package com.telekom.orderPlacement.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter for logging HTTP requests and responses.
 * 
 * Captures:
 * - Request method, URI, query params
 * - Response status and timing
 * - User agent and client IP
 * 
 * Log Structure (JSON):
 * {
 *   "timestamp": "2024-01-15T10:30:00.000Z",
 *   "level": "INFO",
 *   "logger": "RequestLoggingFilter",
 *   "message": "HTTP Request completed",
 *   "correlationId": "abc-123",
 *   "method": "POST",
 *   "path": "/orders/place",
 *   "status": 201,
 *   "durationMs": 45
 * }
 * 
 * Note: Runs after CorrelationIdFilter to have MDC context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Wrap response to capture status after processing
        ContentCachingResponseWrapper wrappedResponse = 
                new ContentCachingResponseWrapper(httpResponse);
        
        long startTime = System.currentTimeMillis();
        
        // Log request start
        logRequestStart(httpRequest);
        
        try {
            // Process request
            chain.doFilter(request, wrappedResponse);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();
            
            // Add response context to MDC
            MDC.put("status", String.valueOf(status));
            MDC.put("durationMs", String.valueOf(duration));
            
            // Log request completion
            logRequestComplete(httpRequest, status, duration);
            
            // Copy body to actual response
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private void logRequestStart(HttpServletRequest request) {
        if (shouldSkipLogging(request)) {
            return;
        }
        
        log.info("HTTP Request started: {} {} query={}", 
                request.getMethod(), 
                request.getRequestURI(),
                request.getQueryString());
    }
    
    private void logRequestComplete(HttpServletRequest request, int status, long duration) {
        if (shouldSkipLogging(request)) {
            return;
        }
        
        String logLevel = status >= 500 ? "ERROR" : (status >= 400 ? "WARN" : "INFO");
        
        if (status >= 500) {
            log.error("HTTP Request completed: {} {} status={} durationMs={}", 
                    request.getMethod(), request.getRequestURI(), status, duration);
        } else if (status >= 400) {
            log.warn("HTTP Request completed: {} {} status={} durationMs={}", 
                    request.getMethod(), request.getRequestURI(), status, duration);
        } else {
            log.info("HTTP Request completed: {} {} status={} durationMs={}", 
                    request.getMethod(), request.getRequestURI(), status, duration);
        }
    }
    
    /**
     * Skip logging for health checks and actuator endpoints.
     */
    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || 
               uri.equals("/health") || 
               uri.equals("/favicon.ico");
    }
}
