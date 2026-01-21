package com.telekom.orderPlacement.repository;

import com.telekom.orderPlacement.domain.model.Order;
import com.telekom.orderPlacement.domain.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for Order persistence.
 * 
 * Thread Safety Strategy:
 * - Uses ConcurrentHashMap for all storage operations
 * - Orders are immutable once created
 * - Status updates create new Order instances
 * 
 * Idempotency Support:
 * - Secondary index by idempotencyKey enables duplicate detection
 * - putIfAbsent provides atomic insert-if-not-exists
 * 
 * Trade-offs:
 * Same as CartRepository - see that class for detailed analysis.
 */
@Repository
public class OrderRepository {
    
    private static final Logger log = LoggerFactory.getLogger(OrderRepository.class);
    
    /**
     * Primary storage: orderId -> Order
     */
    private final ConcurrentHashMap<String, Order> ordersByIdMap = new ConcurrentHashMap<>();
    
    /**
     * Secondary index: userId -> List<orderId>
     * Enables efficient lookup of user's order history.
     * Note: Using ConcurrentHashMap with thread-safe list operations.
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> orderIdsByUserMap = 
            new ConcurrentHashMap<>();
    
    /**
     * Idempotency index: idempotencyKey -> orderId
     * Prevents duplicate order creation for same idempotency key.
     */
    private final ConcurrentHashMap<String, String> orderIdsByIdempotencyKeyMap = 
            new ConcurrentHashMap<>();
    
    /**
     * Saves a new order or updates existing one.
     * 
     * @param order The order to save
     * @return The saved order
     */
    public Order save(Order order) {
        ordersByIdMap.put(order.getOrderId(), order);
        
        // Update user index
        orderIdsByUserMap
                .computeIfAbsent(order.getUserId(), k -> new ConcurrentHashMap<>())
                .put(order.getOrderId(), Boolean.TRUE);
        
        // Update idempotency index
        orderIdsByIdempotencyKeyMap.put(order.getIdempotencyKey(), order.getOrderId());
        
        log.debug("Saved order: orderId={}, userId={}, status={}", 
                order.getOrderId(), order.getUserId(), order.getStatus());
        return order;
    }
    
    /**
     * Atomically creates an order only if the idempotency key doesn't exist.
     * This is the core of idempotent order placement.
     * 
     * Thread Safety:
     * Uses putIfAbsent which is atomic in ConcurrentHashMap.
     * 
     * @param order The order to create
     * @return The order (newly created or existing with same idempotency key)
     */
    public Order saveIfAbsentByIdempotencyKey(Order order) {
        // Atomically check and insert idempotency key
        String existingOrderId = orderIdsByIdempotencyKeyMap.putIfAbsent(
                order.getIdempotencyKey(), order.getOrderId());
        
        if (existingOrderId != null) {
            // Order with this idempotency key already exists
            log.info("Order with idempotency key already exists: key={}, existingOrderId={}", 
                    order.getIdempotencyKey(), existingOrderId);
            return ordersByIdMap.get(existingOrderId);
        }
        
        // New order - save it
        return save(order);
    }
    
    /**
     * Finds an order by its unique identifier.
     * 
     * @param orderId The order ID to search for
     * @return Optional containing the order if found
     */
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(ordersByIdMap.get(orderId));
    }
    
    /**
     * Finds an order by its idempotency key.
     * 
     * @param idempotencyKey The idempotency key to search for
     * @return Optional containing the order if found
     */
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        String orderId = orderIdsByIdempotencyKeyMap.get(idempotencyKey);
        if (orderId == null) {
            return Optional.empty();
        }
        return findById(orderId);
    }
    
    /**
     * Finds all orders for a user.
     * 
     * @param userId The user ID to search for
     * @return List of user's orders
     */
    public List<Order> findByUserId(String userId) {
        ConcurrentHashMap<String, Boolean> orderIds = orderIdsByUserMap.get(userId);
        if (orderIds == null) {
            return List.of();
        }
        
        return orderIds.keySet().stream()
                .map(ordersByIdMap::get)
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds all orders with a specific status.
     * 
     * @param status The status to filter by
     * @return List of orders with the specified status
     */
    public List<Order> findByStatus(OrderStatus status) {
        return ordersByIdMap.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if an order exists with the given ID.
     * 
     * @param orderId The order ID to check
     * @return true if order exists
     */
    public boolean existsById(String orderId) {
        return ordersByIdMap.containsKey(orderId);
    }
    
    /**
     * Checks if an order exists with the given idempotency key.
     * 
     * @param idempotencyKey The idempotency key to check
     * @return true if order with this key exists
     */
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return orderIdsByIdempotencyKeyMap.containsKey(idempotencyKey);
    }
    
    /**
     * Updates an order's status.
     * Creates new immutable Order instance with updated status.
     * 
     * @param orderId The order ID to update
     * @param newStatus The new status
     * @return Updated order if found
     */
    public Optional<Order> updateStatus(String orderId, OrderStatus newStatus) {
        Order[] result = new Order[1];
        
        ordersByIdMap.computeIfPresent(orderId, (key, existing) -> {
            Order updated = existing.withStatus(newStatus);
            result[0] = updated;
            return updated;
        });
        
        if (result[0] != null) {
            log.debug("Updated order status: orderId={}, newStatus={}", orderId, newStatus);
        }
        
        return Optional.ofNullable(result[0]);
    }
    
    /**
     * Returns all orders in the repository.
     * 
     * @return Collection of all orders
     */
    public Collection<Order> findAll() {
        return ordersByIdMap.values();
    }
    
    /**
     * Returns the total number of orders in the repository.
     * 
     * @return Order count
     */
    public long count() {
        return ordersByIdMap.size();
    }
    
    /**
     * Clears all orders from the repository.
     * Warning: For testing purposes only.
     */
    public void clear() {
        ordersByIdMap.clear();
        orderIdsByUserMap.clear();
        orderIdsByIdempotencyKeyMap.clear();
        log.warn("All orders cleared from repository");
    }
}
