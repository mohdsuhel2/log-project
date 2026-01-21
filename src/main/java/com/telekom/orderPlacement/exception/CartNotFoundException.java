package com.telekom.orderPlacement.exception;

/**
 * Exception thrown when a requested cart is not found.
 */
public class CartNotFoundException extends RuntimeException {
    
    private final String cartId;
    
    public CartNotFoundException(String cartId) {
        super("Cart not found: " + cartId);
        this.cartId = cartId;
    }
    
    public String getCartId() {
        return cartId;
    }
}
