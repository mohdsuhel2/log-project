package com.telekom.orderPlacement.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Generic API response wrapper for consistent response structure.
 * 
 * Provides:
 * - Uniform response format across all endpoints
 * - Success/error indication
 * - Correlation ID for request tracing
 * - Timestamp for audit purposes
 * 
 * @param <T> The type of data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String correlationId,
        Instant timestamp,
        ErrorDetails error
) {
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>(true, message, data, correlationId, Instant.now(), null);
    }
    
    /**
     * Creates a successful response with data and default message.
     */
    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return success(data, "Operation completed successfully", correlationId);
    }
    
    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message, ErrorDetails error, String correlationId) {
        return new ApiResponse<>(false, message, null, correlationId, Instant.now(), error);
    }
    
    /**
     * Creates an error response with simple message.
     */
    public static <T> ApiResponse<T> error(String message, String correlationId) {
        return error(message, null, correlationId);
    }
    
    /**
     * Error details record for structured error information.
     */
    public record ErrorDetails(
            String code,
            String description,
            String path,
            java.util.Map<String, String> fieldErrors
    ) {
        public static ErrorDetails of(String code, String description) {
            return new ErrorDetails(code, description, null, null);
        }
        
        public static ErrorDetails of(String code, String description, String path) {
            return new ErrorDetails(code, description, path, null);
        }
        
        public static ErrorDetails withFieldErrors(String code, String description, 
                                                    java.util.Map<String, String> fieldErrors) {
            return new ErrorDetails(code, description, null, fieldErrors);
        }
    }
}
