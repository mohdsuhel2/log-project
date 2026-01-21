package com.telekom.orderPlacement.api.controller;

import com.telekom.orderPlacement.api.dto.request.PlaceOrderRequest;
import com.telekom.orderPlacement.api.dto.response.ApiResponse;
import com.telekom.orderPlacement.api.dto.response.OrderResponse;
import com.telekom.orderPlacement.domain.model.Order;
import com.telekom.orderPlacement.domain.model.OrderStatus;
import com.telekom.orderPlacement.logging.CorrelationIdFilter;
import com.telekom.orderPlacement.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Order operations.
 * 
 * API Endpoints:
 * - POST /orders/place           - Place a new order (idempotent)
 * - GET  /orders/{orderId}       - Get order details
 * - GET  /orders                 - List orders (with optional filters)
 * - PUT  /orders/{orderId}/status - Update order status
 * - POST /orders/{orderId}/cancel - Cancel an order
 * 
 * Idempotency:
 * - POST /orders/place requires an idempotencyKey
 * - Duplicate requests with same key return existing order
 * - Status code 200 for existing, 201 for new
 * 
 * HTTP Status Codes:
 * - 200 OK: Successful GET or duplicate order
 * - 201 Created: New order placed
 * - 400 Bad Request: Validation error or empty cart
 * - 404 Not Found: Order or cart not found
 * - 409 Conflict: Invalid state transition
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Places a new order from a cart.
     * 
     * Idempotency:
     * - Client must provide unique idempotencyKey
     * - Retry-safe: same key returns same order
     * - 201 for new order, 200 for existing
     * 
     * Request:
     * POST /orders/place
     * Content-Type: application/json
     * {
     *   "cartId": "cart-123",
     *   "idempotencyKey": "unique-client-key-456",
     *   "shippingAddress": "123 Main St, City, Country",
     *   "notes": "Please leave at door"
     * }
     * 
     * Response: 201 Created (new) or 200 OK (existing)
     * {
     *   "success": true,
     *   "message": "Order placed successfully",
     *   "data": {
     *     "orderId": "order-789",
     *     "status": "PENDING",
     *     "items": [...],
     *     "total": 59.98
     *   }
     * }
     */
    @PostMapping("/place")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        
        log.info("Place order request: cartId={}, idempotencyKey={}", 
                request.cartId(), request.idempotencyKey());
        
        OrderService.OrderResult result = orderService.placeOrder(request);
        OrderResponse response = OrderResponse.fromOrder(result.order());
        
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        
        if (result.wasExisting()) {
            // Idempotent response - order already existed
            log.info("Returning existing order: orderId={}", result.order().getOrderId());
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success(
                            response,
                            "Order already exists (idempotent response)",
                            correlationId
                    ));
        } else {
            // New order created
            log.info("New order created: orderId={}, total={}", 
                    result.order().getOrderId(), result.order().getTotal());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            response,
                            "Order placed successfully",
                            correlationId
                    ));
        }
    }
    
    /**
     * Retrieves an order by its ID.
     * 
     * Request:
     * GET /orders/{orderId}
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { orderId, status, items, total, ... }
     * }
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderId) {
        
        log.debug("Get order request: orderId={}", orderId);
        
        Order order = orderService.getOrder(orderId);
        OrderResponse response = OrderResponse.fromOrder(order);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Lists orders with optional filtering.
     * 
     * Request:
     * GET /orders?userId=user-123
     * GET /orders?status=PENDING
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": [{ order1 }, { order2 }]
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) OrderStatus status) {
        
        log.debug("List orders request: userId={}, status={}", userId, status);
        
        List<Order> orders;
        if (userId != null) {
            orders = orderService.getOrdersByUserId(userId);
        } else if (status != null) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            // Return empty list if no filter (would be paginated in production)
            orders = List.of();
        }
        
        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::fromOrder)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(
                responses,
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Confirms an order.
     * 
     * Request:
     * POST /orders/{orderId}/confirm
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { order with status=CONFIRMED }
     * }
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable String orderId) {
        
        log.info("Confirm order request: orderId={}", orderId);
        
        Order order = orderService.confirmOrder(orderId);
        OrderResponse response = OrderResponse.fromOrder(order);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Order confirmed",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Cancels an order.
     * Only possible for PENDING or CONFIRMED orders.
     * 
     * Request:
     * POST /orders/{orderId}/cancel
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { order with status=CANCELLED }
     * }
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String orderId) {
        
        log.info("Cancel order request: orderId={}", orderId);
        
        Order order = orderService.cancelOrder(orderId);
        OrderResponse response = OrderResponse.fromOrder(order);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Order cancelled",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Updates order status.
     * Used for processing/shipping/delivery updates.
     * 
     * Request:
     * PUT /orders/{orderId}/status?newStatus=SHIPPED
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { order with updated status }
     * }
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus newStatus) {
        
        log.info("Update order status request: orderId={}, newStatus={}", orderId, newStatus);
        
        Order order = orderService.updateOrderStatus(orderId, newStatus);
        OrderResponse response = OrderResponse.fromOrder(order);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Order status updated to " + newStatus,
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
}
