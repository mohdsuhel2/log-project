package com.telekom.orderPlacement.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter for correlation ID propagation.
 * 
 * Correlation ID Flow:
 * 1. Check incoming request for X-Correlation-ID header
 * 2. If not present, generate new UUID
 * 3. Store in MDC for logging throughout request lifecycle
 * 4. Add to response header for client-side correlation
 * 
 * MDC (Mapped Diagnostic Context):
 * - Thread-local storage for logging context
 * - Automatically included in all log statements
 * - Cleared after request completion to prevent leaks
 * 
 * Usage in Logs:
 * All logs will include: "correlationId": "abc-123-def-456"
 * 
 * Usage in Kibana:
 * Filter by: correlationId: "abc-123-def-456"
 * to see all logs for a single request across services
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Extract or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Generate unique request ID (always new per request)
            String requestId = UUID.randomUUID().toString();
            
            // Store in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            
            // Add additional context
            MDC.put("method", httpRequest.getMethod());
            MDC.put("path", httpRequest.getRequestURI());
            MDC.put("remoteAddr", httpRequest.getRemoteAddr());
            
            // Add correlation ID to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader("X-Request-ID", requestId);
            
            // Continue filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
    
    /**
     * Static utility to get current correlation ID.
     * Useful for Kafka event publishing.
     */
    public static String getCurrentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
    
    /**
     * Static utility to set correlation ID programmatically.
     * Useful for async processing or scheduled tasks.
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }
}
