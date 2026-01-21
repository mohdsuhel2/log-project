package com.telekom.orderPlacement.exception;

import com.telekom.orderPlacement.api.dto.response.ApiResponse;
import com.telekom.orderPlacement.api.dto.response.ApiResponse.ErrorDetails;
import com.telekom.orderPlacement.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across all controllers.
 * 
 * Features:
 * - Structured error responses with error codes
 * - Correlation ID propagation in error responses
 * - Proper HTTP status codes for different error types
 * - All errors are logged (sent to Kafka via Logback)
 * 
 * Error Response Structure:
 * {
 *   "success": false,
 *   "message": "Human readable message",
 *   "correlationId": "abc-123",
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "error": {
 *     "code": "CART_NOT_FOUND",
 *     "description": "Detailed error description",
 *     "path": "/cart/xyz",
 *     "fieldErrors": { "field": "error message" }
 *   }
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // ==================== Business Exceptions ====================
    
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartNotFound(
            CartNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Cart not found: cartId={}, path={}", ex.getCartId(), request.getRequestURI());
        
        ErrorDetails error = new ErrorDetails(
                "CART_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleItemNotFound(
            ItemNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Item not found: itemId={}, cartId={}, path={}", 
                ex.getItemId(), ex.getCartId(), request.getRequestURI());
        
        ErrorDetails error = new ErrorDetails(
                "ITEM_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Order not found: orderId={}, path={}", ex.getOrderId(), request.getRequestURI());
        
        ErrorDetails error = new ErrorDetails(
                "ORDER_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartEmpty(
            CartEmptyException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Attempted to place order from empty cart: cartId={}, path={}", 
                ex.getCartId(), request.getRequestURI());
        
        ErrorDetails error = new ErrorDetails(
                "CART_EMPTY",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateOrder(
            DuplicateOrderException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Duplicate order attempt: idempotencyKey={}, existingOrderId={}", 
                ex.getIdempotencyKey(), ex.getExistingOrderId());
        
        ErrorDetails error = new ErrorDetails(
                "DUPLICATE_ORDER",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    // ==================== Validation Exceptions ====================
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? 
                                error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        
        log.warn("Validation failed: path={}, errors={}", request.getRequestURI(), fieldErrors);
        
        ErrorDetails error = ErrorDetails.withFieldErrors(
                "VALIDATION_ERROR",
                "Request validation failed",
                fieldErrors
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", error, correlationId));
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
                fieldErrors.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );
        
        log.warn("Constraint violation: path={}, errors={}", request.getRequestURI(), fieldErrors);
        
        ErrorDetails error = ErrorDetails.withFieldErrors(
                "CONSTRAINT_VIOLATION",
                "Request constraint violation",
                fieldErrors
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Constraint violation", error, correlationId));
    }
    
    // ==================== HTTP Exceptions ====================
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Malformed JSON request: path={}", request.getRequestURI());
        
        ErrorDetails error = new ErrorDetails(
                "MALFORMED_REQUEST",
                "Request body is not valid JSON",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request body", error, correlationId));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        ErrorDetails error = new ErrorDetails(
                "METHOD_NOT_ALLOWED",
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        ErrorDetails error = new ErrorDetails(
                "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type " + ex.getContentType() + " is not supported",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        ErrorDetails error = new ErrorDetails(
                "ENDPOINT_NOT_FOUND",
                "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found", error, correlationId));
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        ErrorDetails error = new ErrorDetails(
                "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        ErrorDetails error = new ErrorDetails(
                "TYPE_MISMATCH",
                "Parameter '" + ex.getName() + "' has invalid type",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid parameter type", error, correlationId));
    }
    
    // ==================== Concurrency Exceptions ====================
    
    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentModification(
            ConcurrentModificationException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Concurrent modification detected: path={}, error={}", 
                request.getRequestURI(), ex.getMessage());
        
        ErrorDetails error = new ErrorDetails(
                "CONCURRENT_MODIFICATION",
                "Resource was modified by another request. Please retry.",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Concurrent modification detected", error, correlationId));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.warn("Illegal state: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        ErrorDetails error = new ErrorDetails(
                "ILLEGAL_STATE",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error, correlationId));
    }
    
    // ==================== Generic Exception ====================
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        log.error("Unexpected error: path={}, exception={}, message={}", 
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        ErrorDetails error = new ErrorDetails(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                null
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", error, correlationId));
    }
}
