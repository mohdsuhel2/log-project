package com.telekom.orderPlacement.api.dto.response;

import com.telekom.orderPlacement.domain.model.Cart;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for cart operations.
 * 
 * Provides a clean API view of the internal Cart model.
 * Includes computed fields like totalPrice and itemCount.
 */
public record CartResponse(
        String cartId,
        String userId,
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    /**
     * Factory method to convert domain Cart to response DTO.
     */
    public static CartResponse fromCart(Cart cart) {
        List<CartItemResponse> items = cart.getItems().values().stream()
                .map(CartItemResponse::fromCartItem)
                .toList();
        
        return new CartResponse(
                cart.getCartId(),
                cart.getUserId(),
                items,
                cart.getItemCount(),
                cart.getTotalPrice(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                cart.getVersion()
        );
    }
}
