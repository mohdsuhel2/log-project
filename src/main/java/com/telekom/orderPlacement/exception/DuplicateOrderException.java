package com.telekom.orderPlacement.exception;

/**
 * Exception thrown when attempting to place a duplicate order.
 * (Though with idempotency, this shouldn't be thrown - existing order is returned instead)
 */
public class DuplicateOrderException extends RuntimeException {
    
    private final String idempotencyKey;
    private final String existingOrderId;
    
    public DuplicateOrderException(String idempotencyKey, String existingOrderId) {
        super("Order already exists for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
        this.existingOrderId = existingOrderId;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public String getExistingOrderId() {
        return existingOrderId;
    }
}
