package com.telekom.orderPlacement.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an immutable item within an order.
 * 
 * Design Decision:
 * - Completely immutable (no setters, all fields final)
 * - Created from CartItem during order placement
 * - Once order is placed, items cannot be modified
 * 
 * Key Differences from CartItem:
 * - No updatedAt field (immutable after creation)
 * - Captures price at time of order (price snapshot)
 * - Linked to specific order via containment
 */
public final class OrderItem {
    
    private final String itemId;
    private final String sku;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
    
    private OrderItem(String itemId, String sku, String productName, 
                      int quantity, BigDecimal unitPrice) {
        this.itemId = itemId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Factory method to create OrderItem from CartItem.
     * Captures the current state of the cart item at order time.
     */
    public static OrderItem fromCartItem(CartItem cartItem) {
        return new OrderItem(
                cartItem.getItemId(),
                cartItem.getSku(),
                cartItem.getProductName(),
                cartItem.getQuantity(),
                cartItem.getUnitPrice()
        );
    }
    
    // Getters (no setters - immutable)
    public String getItemId() { return itemId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(itemId, orderItem.itemId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
    
    @Override
    public String toString() {
        return "OrderItem{" +
                "itemId='" + itemId + '\'' +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
