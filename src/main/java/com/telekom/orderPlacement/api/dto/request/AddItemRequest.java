package com.telekom.orderPlacement.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for adding an item to cart.
 * 
 * Validation:
 * - sku: Required product identifier
 * - productName: Required human-readable name
 * - quantity: Must be at least 1
 * - unitPrice: Must be non-negative
 */
public record AddItemRequest(
        
        @NotBlank(message = "SKU is required")
        @Size(min = 1, max = 50, message = "SKU must be between 1 and 50 characters")
        String sku,
        
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
        String productName,
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
        BigDecimal unitPrice
        
) {
    /**
     * Factory method with default quantity of 1.
     */
    public static AddItemRequest of(String sku, String productName, BigDecimal unitPrice) {
        return new AddItemRequest(sku, productName, 1, unitPrice);
    }
}
