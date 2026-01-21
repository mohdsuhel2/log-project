package com.telekom.orderPlacement.api.controller;

import com.telekom.orderPlacement.api.dto.request.AddItemRequest;
import com.telekom.orderPlacement.api.dto.request.CreateCartRequest;
import com.telekom.orderPlacement.api.dto.request.UpdateItemRequest;
import com.telekom.orderPlacement.api.dto.response.ApiResponse;
import com.telekom.orderPlacement.api.dto.response.CartResponse;
import com.telekom.orderPlacement.domain.model.Cart;
import com.telekom.orderPlacement.logging.CorrelationIdFilter;
import com.telekom.orderPlacement.service.CartService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Cart operations.
 * 
 * API Endpoints:
 * - POST   /cart                      - Create a new cart
 * - GET    /cart/{cartId}             - Get cart details
 * - POST   /cart/{cartId}/items       - Add item to cart
 * - PUT    /cart/{cartId}/items/{id}  - Update item quantity
 * - DELETE /cart/{cartId}/items/{id}  - Remove item from cart
 * - DELETE /cart/{cartId}             - Delete cart
 * 
 * HTTP Status Codes:
 * - 200 OK: Successful GET/PUT/DELETE
 * - 201 Created: Successful POST (new resource created)
 * - 400 Bad Request: Validation error
 * - 404 Not Found: Cart or item not found
 * - 500 Internal Server Error: Unexpected error
 * 
 * Headers:
 * - X-Correlation-ID: Request correlation ID (auto-generated if not provided)
 * - Content-Type: application/json
 */
@RestController
@RequestMapping("/cart")
public class CartController {
    
    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    
    private final CartService cartService;
    
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    
    /**
     * Creates a new cart for a user.
     * 
     * Request:
     * POST /cart
     * Content-Type: application/json
     * {
     *   "userId": "user-123"
     * }
     * 
     * Response: 201 Created
     * {
     *   "success": true,
     *   "data": { cartId, userId, items, ... },
     *   "correlationId": "abc-123"
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> createCart(
            @Valid @RequestBody CreateCartRequest request) {
        
        log.info("Create cart request: userId={}", request.userId());
        
        Cart cart = cartService.createCart(request);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        response, 
                        "Cart created successfully",
                        CorrelationIdFilter.getCurrentCorrelationId()
                ));
    }
    
    /**
     * Retrieves a cart by its ID.
     * 
     * Request:
     * GET /cart/{cartId}
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { cartId, userId, items, totalPrice, ... }
     * }
     */
    @GetMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @PathVariable String cartId) {
        
        log.debug("Get cart request: cartId={}", cartId);
        
        Cart cart = cartService.getCart(cartId);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Adds an item to the cart.
     * 
     * Request:
     * POST /cart/{cartId}/items
     * Content-Type: application/json
     * {
     *   "sku": "PROD-001",
     *   "productName": "Wireless Mouse",
     *   "quantity": 2,
     *   "unitPrice": 29.99
     * }
     * 
     * Response: 201 Created
     * {
     *   "success": true,
     *   "data": { updated cart }
     * }
     */
    @PostMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @PathVariable String cartId,
            @Valid @RequestBody AddItemRequest request) {
        
        log.info("Add item request: cartId={}, sku={}, quantity={}", 
                cartId, request.sku(), request.quantity());
        
        Cart cart = cartService.addItem(cartId, request);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        response,
                        "Item added to cart",
                        CorrelationIdFilter.getCurrentCorrelationId()
                ));
    }
    
    /**
     * Updates the quantity of an item in the cart.
     * 
     * Request:
     * PUT /cart/{cartId}/items/{itemId}
     * Content-Type: application/json
     * {
     *   "quantity": 5
     * }
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { updated cart }
     * }
     */
    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable String cartId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        
        log.info("Update item request: cartId={}, itemId={}, quantity={}", 
                cartId, itemId, request.quantity());
        
        Cart cart = cartService.updateItemQuantity(cartId, itemId, request);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Item quantity updated",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Removes an item from the cart.
     * 
     * Request:
     * DELETE /cart/{cartId}/items/{itemId}
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { updated cart }
     * }
     */
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String cartId,
            @PathVariable String itemId) {
        
        log.info("Remove item request: cartId={}, itemId={}", cartId, itemId);
        
        Cart cart = cartService.removeItem(cartId, itemId);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Item removed from cart",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Deletes a cart entirely.
     * 
     * Request:
     * DELETE /cart/{cartId}
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "message": "Cart deleted successfully"
     * }
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<ApiResponse<Void>> deleteCart(@PathVariable String cartId) {
        
        log.info("Delete cart request: cartId={}", cartId);
        
        cartService.deleteCart(cartId);
        
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Cart deleted successfully",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
    
    /**
     * Clears all items from a cart.
     * 
     * Request:
     * DELETE /cart/{cartId}/items
     * 
     * Response: 200 OK
     * {
     *   "success": true,
     *   "data": { empty cart }
     * }
     */
    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@PathVariable String cartId) {
        
        log.info("Clear cart request: cartId={}", cartId);
        
        Cart cart = cartService.clearCart(cartId);
        CartResponse response = CartResponse.fromCart(cart);
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Cart cleared",
                CorrelationIdFilter.getCurrentCorrelationId()
        ));
    }
}
