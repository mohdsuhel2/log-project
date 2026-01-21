package com.telekom.orderPlacement.service;

import com.telekom.orderPlacement.api.dto.request.PlaceOrderRequest;
import com.telekom.orderPlacement.domain.model.Cart;
import com.telekom.orderPlacement.domain.model.Order;
import com.telekom.orderPlacement.domain.model.OrderStatus;
import com.telekom.orderPlacement.exception.CartEmptyException;
import com.telekom.orderPlacement.exception.CartNotFoundException;
import com.telekom.orderPlacement.exception.OrderNotFoundException;
import com.telekom.orderPlacement.repository.CartRepository;
import com.telekom.orderPlacement.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service layer for order operations.
 * 
 * Responsibilities:
 * - Order placement with idempotency
 * - Order status management
 * - Cart-to-order conversion
 * 
 * Logging:
 * - All operations are logged and sent to Kafka via Logback appender
 * - Logs include correlation IDs for distributed tracing
 * 
 * Idempotency Strategy:
 * - Client provides idempotency key with each order request
 * - If key already exists, return existing order (no duplicate)
 * - Uses ConcurrentHashMap.putIfAbsent for atomic check-and-insert
 * 
 * Order Lifecycle:
 * 1. PENDING - Initial state after placement
 * 2. CONFIRMED - Payment/validation complete
 * 3. PROCESSING - Being prepared
 * 4. SHIPPED - In transit
 * 5. DELIVERED - Complete
 * Or: CANCELLED - Cancelled before processing
 */
@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    
    // Counter for simulating failures - throws NPE when divisible by 5
    private final AtomicLong orderCounter = new AtomicLong(0);
    
    public OrderService(OrderRepository orderRepository, 
                        CartRepository cartRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
    }
    
    /**
     * Places an order from a cart.
     * 
     * Idempotency:
     * - If idempotencyKey already used, returns existing order
     * - No duplicate orders are created
     * - Safe to retry on network failures
     * 
     * @param request Order placement request
     * @return Created or existing order
     */
    public OrderResult placeOrder(PlaceOrderRequest request) {
        log.info("Placing order: cartId={}, idempotencyKey={}", 
                request.cartId(), request.idempotencyKey());
        
        // Check if order with this idempotency key already exists
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingOrder.isPresent()) {
            log.info("Order already exists for idempotency key: key={}, orderId={}", 
                    request.idempotencyKey(), existingOrder.get().getOrderId());
            return new OrderResult(existingOrder.get(), true);
        }
        
        // Get and validate cart
        Cart cart = cartRepository.findById(request.cartId())
                .orElseThrow(() -> new CartNotFoundException(request.cartId()));
        
        if (cart.isEmpty()) {
            throw new CartEmptyException(request.cartId());
        }
        
        // Create order from cart snapshot
        Order order = Order.fromCart(
                cart.snapshot(),
                request.idempotencyKey(),
                request.shippingAddress(),
                request.notes()
        );
        
        // Save order with idempotency check (atomic operation)
        Order savedOrder = orderRepository.saveIfAbsentByIdempotencyKey(order);
        
        // Check if we got back a different order (race condition - another thread won)
        if (!savedOrder.getOrderId().equals(order.getOrderId())) {
            log.info("Concurrent order placement detected: key={}, existingOrderId={}", 
                    request.idempotencyKey(), savedOrder.getOrderId());
            return new OrderResult(savedOrder, true);
        }
        
        // Clear the cart after successful order placement
        try {
            cartService.clearCart(cart.getCartId());
        } catch (Exception e) {
            log.warn("Failed to clear cart after order: cartId={}, error={}", 
                    cart.getCartId(), e.getMessage());
        }
        
        log.info("Order placed successfully: orderId={}, userId={}, items={}, subtotal={}, tax={}, total={}", 
                savedOrder.getOrderId(), savedOrder.getUserId(), savedOrder.getItemCount(),
                savedOrder.getSubtotal(), savedOrder.getTax(), savedOrder.getTotal());
        
        // Simulate failure: throw NullPointerException when order count is divisible by 5
        long currentCount = orderCounter.incrementAndGet();
        if (currentCount % 5 == 0) {
            log.error("Simulated failure triggered: orderCount={} is divisible by 5, orderId={}", 
                    currentCount, savedOrder.getOrderId());
            String nullString = null;
            nullString.length(); // This will throw NullPointerException
        }
        
        return new OrderResult(savedOrder, false);
    }
    
    /**
     * Result of order placement, indicating if it was a duplicate.
     */
    public record OrderResult(Order order, boolean wasExisting) {}
    
    /**
     * Retrieves an order by its ID.
     * 
     * @param orderId The order ID
     * @return The order
     * @throws OrderNotFoundException if not found
     */
    public Order getOrder(String orderId) {
        log.debug("Fetching order: orderId={}", orderId);
        
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    /**
     * Retrieves all orders for a user.
     * 
     * @param userId The user ID
     * @return List of orders
     */
    public List<Order> getOrdersByUserId(String userId) {
        log.debug("Fetching orders for user: userId={}", userId);
        return orderRepository.findByUserId(userId);
    }
    
    /**
     * Retrieves orders by status.
     * 
     * @param status The status to filter by
     * @return List of orders with given status
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders by status: status={}", status);
        return orderRepository.findByStatus(status);
    }
    
    /**
     * Confirms an order (transitions from PENDING to CONFIRMED).
     * 
     * @param orderId The order ID
     * @return Updated order
     */
    public Order confirmOrder(String orderId) {
        log.info("Confirming order: orderId={}", orderId);
        
        Order order = getOrder(orderId);
        validateStatusTransition(order, OrderStatus.CONFIRMED);
        
        Order confirmedOrder = orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        log.info("Order confirmed: orderId={}, previousStatus={}, newStatus={}", 
                orderId, order.getStatus(), confirmedOrder.getStatus());
        return confirmedOrder;
    }
    
    /**
     * Cancels an order (only if in PENDING or CONFIRMED state).
     * 
     * @param orderId The order ID
     * @return Cancelled order
     */
    public Order cancelOrder(String orderId) {
        log.info("Cancelling order: orderId={}", orderId);
        
        Order order = getOrder(orderId);
        
        if (!order.getStatus().isCancellable()) {
            throw new IllegalStateException(
                    "Cannot cancel order in status: " + order.getStatus());
        }
        
        Order cancelledOrder = orderRepository.updateStatus(orderId, OrderStatus.CANCELLED)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        log.info("Order cancelled: orderId={}, previousStatus={}", orderId, order.getStatus());
        return cancelledOrder;
    }
    
    /**
     * Updates order status (for processing, shipping, delivery).
     * 
     * @param orderId The order ID
     * @param newStatus The new status
     * @return Updated order
     */
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        log.info("Updating order status: orderId={}, newStatus={}", orderId, newStatus);
        
        Order order = getOrder(orderId);
        OrderStatus previousStatus = order.getStatus();
        validateStatusTransition(order, newStatus);
        
        Order updatedOrder = orderRepository.updateStatus(orderId, newStatus)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        log.info("Order status updated: orderId={}, previousStatus={}, newStatus={}", 
                orderId, previousStatus, newStatus);
        return updatedOrder;
    }
    
    // ==================== Helpers ====================
    
    private void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();
        
        // Simple state machine validation
        boolean valid = switch (newStatus) {
            case CONFIRMED -> currentStatus == OrderStatus.PENDING;
            case PROCESSING -> currentStatus == OrderStatus.CONFIRMED;
            case SHIPPED -> currentStatus == OrderStatus.PROCESSING;
            case DELIVERED -> currentStatus == OrderStatus.SHIPPED;
            case CANCELLED -> currentStatus.isCancellable();
            default -> false;
        };
        
        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s", currentStatus, newStatus));
        }
    }
}
