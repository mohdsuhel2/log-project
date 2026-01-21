package com.telekom.orderPlacement.service;

import com.telekom.orderPlacement.api.dto.request.AddItemRequest;
import com.telekom.orderPlacement.api.dto.request.CreateCartRequest;
import com.telekom.orderPlacement.api.dto.request.UpdateItemRequest;
import com.telekom.orderPlacement.domain.model.Cart;
import com.telekom.orderPlacement.domain.model.CartItem;
import com.telekom.orderPlacement.exception.CartNotFoundException;
import com.telekom.orderPlacement.exception.ItemNotFoundException;
import com.telekom.orderPlacement.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service layer for cart operations.
 * 
 * Responsibilities:
 * - Business logic for cart management
 * - Validation of business rules
 * - Coordination with repository
 * 
 * Logging:
 * - All operations are logged and sent to Kafka via Logback appender
 * - Logs include correlation IDs for distributed tracing
 * 
 * Thread Safety:
 * - Delegates to thread-safe repository
 * - Stateless service (no instance state)
 * - Safe for concurrent access
 */
@Service
public class CartService {
    
    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    
    private final CartRepository cartRepository;
    
    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }
    
    /**
     * Creates a new cart for a user.
     * 
     * Business Rules:
     * - User can have only one active cart at a time
     * - Previous cart is implicitly replaced
     * 
     * @param request Cart creation request
     * @return The created cart
     */
    public Cart createCart(CreateCartRequest request) {
        log.info("Creating cart for user: userId={}", request.userId());
        
        // Check if user already has a cart
        Optional<Cart> existingCart = cartRepository.findByUserId(request.userId());
        existingCart.ifPresent(cart -> {
            log.info("User already has cart, will be replaced: existingCartId={}", cart.getCartId());
        });
        
        Cart cart = new Cart(request.userId());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Cart created successfully: cartId={}, userId={}", 
                savedCart.getCartId(), savedCart.getUserId());
        
        return savedCart;
    }
    
    /**
     * Retrieves a cart by its ID.
     * 
     * @param cartId The cart ID
     * @return The cart
     * @throws CartNotFoundException if cart not found
     */
    public Cart getCart(String cartId) {
        log.debug("Fetching cart: cartId={}", cartId);
        
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
    }
    
    /**
     * Retrieves a cart by user ID.
     * 
     * @param userId The user ID
     * @return Optional containing cart if found
     */
    public Optional<Cart> getCartByUserId(String userId) {
        log.debug("Fetching cart for user: userId={}", userId);
        return cartRepository.findByUserId(userId);
    }
    
    /**
     * Adds an item to the cart.
     * 
     * Concurrency:
     * - Uses ConcurrentHashMap.compute for atomic add
     * - If same SKU exists, quantities are merged
     * 
     * @param cartId The cart ID
     * @param request Item details
     * @return Updated cart
     */
    public Cart addItem(String cartId, AddItemRequest request) {
        log.info("Adding item to cart: cartId={}, sku={}, quantity={}, unitPrice={}", 
                cartId, request.sku(), request.quantity(), request.unitPrice());
        
        Cart cart = getCart(cartId);
        
        CartItem item = CartItem.builder()
                .sku(request.sku())
                .productName(request.productName())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();
        
        CartItem addedItem = cart.addItem(item);
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Item added successfully: cartId={}, itemId={}, sku={}, totalPrice={}", 
                cartId, addedItem.getItemId(), addedItem.getSku(), savedCart.getTotalPrice());
        
        return savedCart;
    }
    
    /**
     * Updates item quantity in the cart.
     * 
     * @param cartId The cart ID
     * @param itemId The item ID
     * @param request Update request with new quantity
     * @return Updated cart
     */
    public Cart updateItemQuantity(String cartId, String itemId, UpdateItemRequest request) {
        log.info("Updating item quantity: cartId={}, itemId={}, newQuantity={}", 
                cartId, itemId, request.quantity());
        
        Cart cart = getCart(cartId);
        
        CartItem updatedItem = cart.updateItemQuantity(itemId, request.quantity());
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Item quantity updated: cartId={}, itemId={}, quantity={}, totalPrice={}", 
                cartId, itemId, request.quantity(), savedCart.getTotalPrice());
        
        return savedCart;
    }
    
    /**
     * Removes an item from the cart.
     * 
     * @param cartId The cart ID
     * @param itemId The item ID to remove
     * @return Updated cart
     * @throws ItemNotFoundException if item not found
     */
    public Cart removeItem(String cartId, String itemId) {
        log.info("Removing item from cart: cartId={}, itemId={}", cartId, itemId);
        
        Cart cart = getCart(cartId);
        
        CartItem removedItem = cart.removeItem(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId, cartId));
        
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Item removed successfully: cartId={}, itemId={}, sku={}, remainingItems={}", 
                cartId, itemId, removedItem.getSku(), savedCart.getItemCount());
        
        return savedCart;
    }
    
    /**
     * Clears all items from the cart.
     * Used internally when converting cart to order.
     * 
     * @param cartId The cart ID
     * @return Cleared cart
     */
    public Cart clearCart(String cartId) {
        log.info("Clearing cart: cartId={}", cartId);
        
        Cart cart = getCart(cartId);
        int previousItemCount = cart.getItemCount();
        cart.clear();
        Cart savedCart = cartRepository.save(cart);
        
        log.info("Cart cleared: cartId={}, itemsRemoved={}", cartId, previousItemCount);
        
        return savedCart;
    }
    
    /**
     * Deletes a cart entirely.
     * 
     * @param cartId The cart ID
     */
    public void deleteCart(String cartId) {
        log.info("Deleting cart: cartId={}", cartId);
        
        cartRepository.deleteById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        
        log.info("Cart deleted: cartId={}", cartId);
    }
}
