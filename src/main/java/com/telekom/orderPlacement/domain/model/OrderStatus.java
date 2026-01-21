package com.telekom.orderPlacement.domain.model;

/**
 * Represents the lifecycle states of an Order.
 * 
 * State Machine:
 * PENDING -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
 *                     \-> CANCELLED
 * 
 * Design Decision:
 * - Using enum for type safety and preventing invalid states
 * - Once an order moves past CONFIRMED, it cannot be cancelled (immutability principle)
 */
public enum OrderStatus {
    
    /**
     * Initial state when order is first created from cart.
     * Order is awaiting confirmation.
     */
    PENDING("Order is pending confirmation"),
    
    /**
     * Order has been confirmed and payment verified.
     * This is the point of no return - order becomes immutable.
     */
    CONFIRMED("Order has been confirmed"),
    
    /**
     * Order is being processed/prepared for shipping.
     */
    PROCESSING("Order is being processed"),
    
    /**
     * Order has been shipped to the customer.
     */
    SHIPPED("Order has been shipped"),
    
    /**
     * Order has been delivered to the customer.
     */
    DELIVERED("Order has been delivered"),
    
    /**
     * Order was cancelled before processing.
     */
    CANCELLED("Order has been cancelled");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the order can be cancelled from this state.
     * Only PENDING and CONFIRMED orders can be cancelled.
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }
    
    /**
     * Checks if the order is in a terminal state.
     * Terminal states: DELIVERED, CANCELLED
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
    
    /**
     * Checks if order modifications are allowed in this state.
     * Once confirmed, orders become immutable.
     */
    public boolean isModifiable() {
        return this == PENDING;
    }
}
