package com.telekom.orderPlacement.exception;

/**
 * Exception thrown when attempting to place an order from an empty cart.
 */
public class CartEmptyException extends RuntimeException {
    
    private final String cartId;
    
    public CartEmptyException(String cartId) {
        super("Cannot place order from empty cart: " + cartId);
        this.cartId = cartId;
    }
    
    public String getCartId() {
        return cartId;
    }
}
