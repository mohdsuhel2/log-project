package com.telekom.orderPlacement.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a shopping cart containing items.
 * 
 * Design Decisions:
 * - Uses ConcurrentHashMap for thread-safe item storage
 * - Version field for optimistic locking in concurrent scenarios
 * - Immutable external view of items (defensive copy via unmodifiableMap)
 * 
 * Thread Safety:
 * - ConcurrentHashMap provides atomic operations for item management
 * - Version field uses volatile for visibility across threads
 * - All mutations update the version to detect concurrent modifications
 * 
 * Trade-offs vs Persistent Storage:
 * - Pros: No I/O latency, simple setup, fast prototyping
 * - Cons: Data loss on restart, limited by heap memory, no ACID transactions
 * - Mitigation: Kafka events provide audit trail for recovery
 */
public class Cart {
    
    private final String cartId;
    private final String userId;
    private final ConcurrentHashMap<String, CartItem> items;
    private final Instant createdAt;
    private volatile Instant updatedAt;
    private volatile long version;
    
    /**
     * Creates a new empty cart for a user.
     */
    public Cart(String userId) {
        this.cartId = UUID.randomUUID().toString();
        this.userId = userId;
        this.items = new ConcurrentHashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }
    
    /**
     * Private constructor for internal use (e.g., copying).
     */
    private Cart(String cartId, String userId, Map<String, CartItem> items, 
                 Instant createdAt, Instant updatedAt, long version) {
        this.cartId = cartId;
        this.userId = userId;
        this.items = new ConcurrentHashMap<>(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
    
    // Getters
    public String getCartId() { return cartId; }
    public String getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    
    /**
     * Returns an unmodifiable view of cart items.
     * Provides encapsulation while allowing read access.
     */
    public Map<String, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
    
    /**
     * Gets a specific item by its ID.
     */
    public Optional<CartItem> getItem(String itemId) {
        return Optional.ofNullable(items.get(itemId));
    }
    
    /**
     * Adds an item to the cart.
     * If item with same SKU exists, updates quantity.
     * 
     * Thread Safety: Uses ConcurrentHashMap.compute for atomic operation.
     * 
     * @return The added or updated CartItem
     */
    public CartItem addItem(CartItem item) {
        CartItem result = items.compute(item.getItemId(), (key, existing) -> {
            if (existing != null && existing.getSku().equals(item.getSku())) {
                // Same SKU exists, merge quantities
                return existing.withQuantity(existing.getQuantity() + item.getQuantity());
            }
            return item;
        });
        incrementVersion();
        return result;
    }
    
    /**
     * Updates quantity of an existing item.
     * 
     * @throws IllegalArgumentException if item doesn't exist
     */
    public CartItem updateItemQuantity(String itemId, int newQuantity) {
        CartItem result = items.computeIfPresent(itemId, (key, existing) -> 
            existing.withQuantity(newQuantity)
        );
        if (result == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        incrementVersion();
        return result;
    }
    
    /**
     * Removes an item from the cart.
     * 
     * @return The removed item, or empty if not found
     */
    public Optional<CartItem> removeItem(String itemId) {
        CartItem removed = items.remove(itemId);
        if (removed != null) {
            incrementVersion();
        }
        return Optional.ofNullable(removed);
    }
    
    /**
     * Clears all items from the cart.
     * Used when converting cart to order.
     */
    public void clear() {
        items.clear();
        incrementVersion();
    }
    
    /**
     * Calculates the total price of all items in cart.
     */
    public BigDecimal getTotalPrice() {
        return items.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Returns the total number of items in cart.
     */
    public int getItemCount() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    /**
     * Checks if the cart is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Increments version and updates timestamp.
     * Used for optimistic concurrency control.
     */
    private void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Creates a snapshot copy of this cart.
     * Useful for creating immutable order from cart.
     */
    public Cart snapshot() {
        return new Cart(cartId, userId, items, createdAt, updatedAt, version);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return Objects.equals(cartId, cart.cartId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cartId);
    }
    
    @Override
    public String toString() {
        return "Cart{" +
                "cartId='" + cartId + '\'' +
                ", userId='" + userId + '\'' +
                ", itemCount=" + getItemCount() +
                ", totalPrice=" + getTotalPrice() +
                ", version=" + version +
                '}';
    }
}
