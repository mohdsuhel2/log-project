package com.telekom.orderPlacement.api.dto.response;

import com.telekom.orderPlacement.domain.model.CartItem;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for cart item data.
 */
public record CartItemResponse(
        String itemId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Instant addedAt,
        Instant updatedAt
) {
    /**
     * Factory method to convert domain CartItem to response DTO.
     */
    public static CartItemResponse fromCartItem(CartItem item) {
        return new CartItemResponse(
                item.getItemId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getAddedAt(),
                item.getUpdatedAt()
        );
    }
}
