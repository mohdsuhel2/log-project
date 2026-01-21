package com.telekom.orderPlacement.exception;

/**
 * Exception thrown when a requested cart item is not found.
 */
public class ItemNotFoundException extends RuntimeException {
    
    private final String itemId;
    private final String cartId;
    
    public ItemNotFoundException(String itemId, String cartId) {
        super("Item not found: " + itemId + " in cart: " + cartId);
        this.itemId = itemId;
        this.cartId = cartId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getCartId() {
        return cartId;
    }
}
