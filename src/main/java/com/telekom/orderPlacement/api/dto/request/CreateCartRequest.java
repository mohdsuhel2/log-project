package com.telekom.orderPlacement.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new cart.
 * 
 * Validation:
 * - userId: Required, identifies the cart owner
 */
public record CreateCartRequest(
        
        @NotBlank(message = "User ID is required")
        @Size(min = 1, max = 100, message = "User ID must be between 1 and 100 characters")
        String userId
        
) {}
