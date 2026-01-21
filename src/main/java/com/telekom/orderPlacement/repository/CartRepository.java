package com.telekom.orderPlacement.repository;

import com.telekom.orderPlacement.domain.model.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for Cart persistence.
 * 
 * Thread Safety Strategy:
 * - Uses ConcurrentHashMap for all storage operations
 * - ConcurrentHashMap provides:
 *   - Atomic putIfAbsent, compute, computeIfPresent operations
 *   - Lock-free reads for high concurrency
 *   - Segmented locking for writes (better than synchronized HashMap)
 * 
 * Trade-offs vs Persistent Storage:
 * 
 * Advantages:
 * - Zero I/O latency (sub-microsecond operations)
 * - No external dependencies (database, connection pools)
 * - Simple deployment and testing
 * - Perfect for prototyping and development
 * 
 * Disadvantages:
 * - Data loss on application restart
 * - Limited by JVM heap memory
 * - No ACID transactions across multiple operations
 * - No built-in replication or persistence
 * 
 * Mitigation Strategies:
 * - Kafka events provide audit trail for data recovery
 * - Periodic snapshots could be added for disaster recovery
 * - Production would use Redis/Postgres with similar interface
 */
@Repository
public class CartRepository {
    
    private static final Logger log = LoggerFactory.getLogger(CartRepository.class);
    
    /**
     * Primary storage: cartId -> Cart
     * ConcurrentHashMap chosen for:
     * - O(1) average case lookups
     * - Thread-safe without external synchronization
     * - Better concurrent write performance than synchronized collections
     */
    private final ConcurrentHashMap<String, Cart> cartsByIdMap = new ConcurrentHashMap<>();
    
    /**
     * Secondary index: userId -> cartId (most recent cart per user)
     * Enables efficient lookup of user's current cart.
     */
    private final ConcurrentHashMap<String, String> cartIdsByUserMap = new ConcurrentHashMap<>();
    
    /**
     * Saves a cart to the repository.
     * 
     * @param cart The cart to save
     * @return The saved cart
     */
    public Cart save(Cart cart) {
        cartsByIdMap.put(cart.getCartId(), cart);
        cartIdsByUserMap.put(cart.getUserId(), cart.getCartId());
        log.debug("Saved cart: cartId={}, userId={}, itemCount={}", 
                cart.getCartId(), cart.getUserId(), cart.getItemCount());
        return cart;
    }
    
    /**
     * Finds a cart by its unique identifier.
     * 
     * @param cartId The cart ID to search for
     * @return Optional containing the cart if found
     */
    public Optional<Cart> findById(String cartId) {
        return Optional.ofNullable(cartsByIdMap.get(cartId));
    }
    
    /**
     * Finds the most recent cart for a user.
     * 
     * @param userId The user ID to search for
     * @return Optional containing the user's cart if found
     */
    public Optional<Cart> findByUserId(String userId) {
        String cartId = cartIdsByUserMap.get(userId);
        if (cartId == null) {
            return Optional.empty();
        }
        return findById(cartId);
    }
    
    /**
     * Checks if a cart exists with the given ID.
     * 
     * @param cartId The cart ID to check
     * @return true if cart exists
     */
    public boolean existsById(String cartId) {
        return cartsByIdMap.containsKey(cartId);
    }
    
    /**
     * Deletes a cart by its ID.
     * 
     * @param cartId The cart ID to delete
     * @return Optional containing the deleted cart if it existed
     */
    public Optional<Cart> deleteById(String cartId) {
        Cart removed = cartsByIdMap.remove(cartId);
        if (removed != null) {
            // Clean up user index
            cartIdsByUserMap.remove(removed.getUserId(), cartId);
            log.debug("Deleted cart: cartId={}", cartId);
        }
        return Optional.ofNullable(removed);
    }
    
    /**
     * Returns all carts in the repository.
     * Note: For debugging/admin purposes only. Not efficient for large datasets.
     * 
     * @return Collection of all carts
     */
    public Collection<Cart> findAll() {
        return cartsByIdMap.values();
    }
    
    /**
     * Returns the total number of carts in the repository.
     * 
     * @return Cart count
     */
    public long count() {
        return cartsByIdMap.size();
    }
    
    /**
     * Clears all carts from the repository.
     * Warning: For testing purposes only.
     */
    public void clear() {
        cartsByIdMap.clear();
        cartIdsByUserMap.clear();
        log.warn("All carts cleared from repository");
    }
    
    /**
     * Atomically updates a cart using optimistic locking.
     * 
     * Thread Safety:
     * Uses ConcurrentHashMap.compute() which is atomic.
     * Version check prevents lost updates from concurrent modifications.
     * 
     * @param cartId The cart ID to update
     * @param expectedVersion The expected version for optimistic locking
     * @param updater Function to apply updates
     * @return Updated cart if successful
     * @throws ConcurrentModificationException if version mismatch
     */
    public Optional<Cart> updateWithVersionCheck(String cartId, long expectedVersion, 
                                                   java.util.function.Function<Cart, Cart> updater) {
        Cart[] result = new Cart[1];
        
        cartsByIdMap.compute(cartId, (key, existing) -> {
            if (existing == null) {
                return null;
            }
            if (existing.getVersion() != expectedVersion) {
                throw new java.util.ConcurrentModificationException(
                        "Cart was modified by another request. Expected version: " + 
                        expectedVersion + ", actual: " + existing.getVersion());
            }
            result[0] = updater.apply(existing);
            return result[0];
        });
        
        return Optional.ofNullable(result[0]);
    }
}
