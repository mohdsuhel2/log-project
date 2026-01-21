package com.telekom.orderPlacement.api.dto.response;

import com.telekom.orderPlacement.domain.model.OrderItem;
import java.math.BigDecimal;

/**
 * Response DTO for order item data.
 */
public record OrderItemResponse(
        String itemId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    /**
     * Factory method to convert domain OrderItem to response DTO.
     */
    public static OrderItemResponse fromOrderItem(OrderItem item) {
        return new OrderItemResponse(
                item.getItemId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
