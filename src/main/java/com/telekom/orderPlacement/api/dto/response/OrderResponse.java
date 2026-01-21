package com.telekom.orderPlacement.api.dto.response;

import com.telekom.orderPlacement.domain.model.Order;
import com.telekom.orderPlacement.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for order operations.
 * 
 * Provides a clean API view of the internal Order model.
 */
public record OrderResponse(
        String orderId,
        String userId,
        String cartId,
        OrderStatus status,
        String statusDescription,
        List<OrderItemResponse> items,
        int itemCount,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String shippingAddress,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory method to convert domain Order to response DTO.
     */
    public static OrderResponse fromOrder(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromOrderItem)
                .toList();
        
        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getCartId(),
                order.getStatus(),
                order.getStatus().getDescription(),
                items,
                order.getItemCount(),
                order.getSubtotal(),
                order.getTax(),
                order.getTotal(),
                order.getShippingAddress(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
