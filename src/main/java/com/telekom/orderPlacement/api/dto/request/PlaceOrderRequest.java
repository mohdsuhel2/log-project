package com.telekom.orderPlacement.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for placing an order from a cart.
 * 
 * Idempotency:
 * - idempotencyKey ensures exactly-once order placement
 * - Same key returns existing order instead of creating duplicate
 * - Client should generate unique key (UUID recommended)
 * 
 * Validation:
 * - cartId: Required, identifies the cart to convert
 * - idempotencyKey: Required for idempotent operation
 * - shippingAddress: Required delivery address
 */
public record PlaceOrderRequest(
        
        @NotBlank(message = "Cart ID is required")
        String cartId,
        
        @NotBlank(message = "Idempotency key is required for order placement")
        @Size(min = 1, max = 100, message = "Idempotency key must be between 1 and 100 characters")
        String idempotencyKey,
        
        @NotBlank(message = "Shipping address is required")
        @Size(min = 10, max = 500, message = "Shipping address must be between 10 and 500 characters")
        String shippingAddress,
        
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
        
) {}
