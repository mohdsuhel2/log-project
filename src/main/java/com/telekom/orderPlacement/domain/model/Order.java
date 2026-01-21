package com.telekom.orderPlacement.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents an immutable order created from a cart.
 * 
 * Design Decisions:
 * - Immutable after creation (all fields final, unmodifiable collections)
 * - Only status can transition, but via controlled state machine
 * - Contains snapshot of items at order time (price lock)
 * - Idempotency key ensures exactly-once order placement
 * 
 * Immutability Rationale:
 * - Prevents accidental modifications after order placement
 * - Thread-safe by design
 * - Audit trail integrity (what was ordered stays ordered)
 * - Simplifies reasoning about order state
 * 
 * Note: Status transitions are handled via OrderService,
 * which creates new Order instances with updated status.
 */
public final class Order {
    
    private final String orderId;
    private final String userId;
    private final String cartId;
    private final String idempotencyKey;
    private final List<OrderItem> items;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String shippingAddress;
    private final String notes;
    
    private Order(Builder builder) {
        this.orderId = builder.orderId;
        this.userId = builder.userId;
        this.cartId = builder.cartId;
        this.idempotencyKey = builder.idempotencyKey;
        this.items = Collections.unmodifiableList(builder.items);
        this.subtotal = builder.subtotal;
        this.tax = builder.tax;
        this.total = builder.total;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.shippingAddress = builder.shippingAddress;
        this.notes = builder.notes;
    }
    
    /**
     * Factory method to create an Order from a Cart.
     * 
     * @param cart The cart to convert
     * @param idempotencyKey Unique key for idempotent order placement
     * @param shippingAddress Delivery address
     * @param notes Optional order notes
     * @return New Order in PENDING status
     */
    public static Order fromCart(Cart cart, String idempotencyKey, 
                                  String shippingAddress, String notes) {
        List<OrderItem> orderItems = cart.getItems().values().stream()
                .map(OrderItem::fromCartItem)
                .collect(Collectors.toList());
        
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Default tax rate: 10% (would be configurable in production)
        BigDecimal taxRate = new BigDecimal("0.10");
        BigDecimal tax = subtotal.multiply(taxRate);
        BigDecimal total = subtotal.add(tax);
        
        return new Builder()
                .orderId(UUID.randomUUID().toString())
                .userId(cart.getUserId())
                .cartId(cart.getCartId())
                .idempotencyKey(idempotencyKey)
                .items(orderItems)
                .subtotal(subtotal)
                .tax(tax)
                .total(total)
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .shippingAddress(shippingAddress)
                .notes(notes)
                .build();
    }
    
    /**
     * Creates a new Order with updated status.
     * Maintains immutability by returning new instance.
     */
    public Order withStatus(OrderStatus newStatus) {
        return new Builder()
                .orderId(this.orderId)
                .userId(this.userId)
                .cartId(this.cartId)
                .idempotencyKey(this.idempotencyKey)
                .items(List.copyOf(this.items))
                .subtotal(this.subtotal)
                .tax(this.tax)
                .total(this.total)
                .status(newStatus)
                .createdAt(this.createdAt)
                .updatedAt(Instant.now())
                .shippingAddress(this.shippingAddress)
                .notes(this.notes)
                .build();
    }
    
    // Getters (no setters - immutable)
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getCartId() { return cartId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getShippingAddress() { return shippingAddress; }
    public String getNotes() { return notes; }
    
    /**
     * Returns the number of unique items in the order.
     */
    public int getItemCount() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", status=" + status +
                ", total=" + total +
                ", itemCount=" + getItemCount() +
                '}';
    }
    
    // Builder for controlled construction
    public static class Builder {
        private String orderId;
        private String userId;
        private String cartId;
        private String idempotencyKey;
        private List<OrderItem> items = Collections.emptyList();
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        private OrderStatus status = OrderStatus.PENDING;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private String shippingAddress;
        private String notes;
        
        public Builder orderId(String orderId) { this.orderId = orderId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder cartId(String cartId) { this.cartId = cartId; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder items(List<OrderItem> items) { this.items = items; return this; }
        public Builder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public Builder tax(BigDecimal tax) { this.tax = tax; return this; }
        public Builder total(BigDecimal total) { this.total = total; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder shippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        
        public Order build() {
            Objects.requireNonNull(orderId, "Order ID cannot be null");
            Objects.requireNonNull(userId, "User ID cannot be null");
            Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
            Objects.requireNonNull(status, "Status cannot be null");
            if (items.isEmpty()) {
                throw new IllegalStateException("Order must have at least one item");
            }
            return new Order(this);
        }
    }
}
