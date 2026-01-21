package com.telekom.orderPlacement.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an item within a shopping cart.
 * 
 * Design Decisions:
 * - Immutable class pattern with builder for thread safety
 * - Using BigDecimal for price to avoid floating-point precision issues
 * - Each cart item has its own unique ID for independent operations
 * - SKU (Stock Keeping Unit) represents the product identifier
 * 
 * Thread Safety:
 * - All fields are final and immutable
 * - Safe to share across threads without synchronization
 */
public final class CartItem {
    
    private final String itemId;
    private final String sku;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final Instant addedAt;
    private final Instant updatedAt;
    
    private CartItem(Builder builder) {
        this.itemId = builder.itemId;
        this.sku = builder.sku;
        this.productName = builder.productName;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.addedAt = builder.addedAt;
        this.updatedAt = builder.updatedAt;
    }
    
    // Getters
    public String getItemId() { return itemId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    /**
     * Calculates the total price for this cart item.
     * Total = unitPrice × quantity
     */
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Creates a new CartItem with updated quantity.
     * Returns new instance (immutability pattern).
     */
    public CartItem withQuantity(int newQuantity) {
        return new Builder()
                .itemId(this.itemId)
                .sku(this.sku)
                .productName(this.productName)
                .quantity(newQuantity)
                .unitPrice(this.unitPrice)
                .addedAt(this.addedAt)
                .updatedAt(Instant.now())
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(itemId, cartItem.itemId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
    
    @Override
    public String toString() {
        return "CartItem{" +
                "itemId='" + itemId + '\'' +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                '}';
    }
    
    // Builder pattern for flexible object construction
    public static class Builder {
        private String itemId = UUID.randomUUID().toString();
        private String sku;
        private String productName;
        private int quantity = 1;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private Instant addedAt = Instant.now();
        private Instant updatedAt = Instant.now();
        
        public Builder itemId(String itemId) { this.itemId = itemId; return this; }
        public Builder sku(String sku) { this.sku = sku; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder addedAt(Instant addedAt) { this.addedAt = addedAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public CartItem build() {
            Objects.requireNonNull(sku, "SKU cannot be null");
            Objects.requireNonNull(productName, "Product name cannot be null");
            if (quantity < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
            return new CartItem(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
