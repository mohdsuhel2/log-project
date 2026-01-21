package com.telekom.orderPlacement.scheduler;

import com.telekom.orderPlacement.api.dto.request.AddItemRequest;
import com.telekom.orderPlacement.api.dto.request.CreateCartRequest;
import com.telekom.orderPlacement.api.dto.request.PlaceOrderRequest;
import com.telekom.orderPlacement.domain.model.Cart;
import com.telekom.orderPlacement.domain.model.Order;
import com.telekom.orderPlacement.logging.CorrelationIdFilter;
import com.telekom.orderPlacement.service.CartService;
import com.telekom.orderPlacement.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled task that places orders automatically every 2 seconds.
 * 
 * This scheduler:
 * 1. Creates a new cart with a random user ID
 * 2. Adds random sample items to the cart
 * 3. Places an order from the cart
 * 
 * Useful for:
 * - Load testing
 * - Generating sample data
 * - Demonstrating Kafka event flow
 * - Populating Kibana dashboards
 */
@Component
public class OrderPlacementScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(OrderPlacementScheduler.class);
    
    private final CartService cartService;
    private final OrderService orderService;
    
    private final AtomicLong orderCounter = new AtomicLong(0);
    private final Random random = new Random();
    
    @Value("${app.scheduler.order-placement.enabled:true}")
    private boolean enabled;
    
    // Sample products for random selection
    private static final String[][] SAMPLE_PRODUCTS = {
            {"LAPTOP-001", "MacBook Pro 16", "2499.00"},
            {"PHONE-001", "iPhone 15 Pro", "1199.00"},
            {"TABLET-001", "iPad Pro 12.9", "1099.00"},
            {"WATCH-001", "Apple Watch Ultra", "799.00"},
            {"HEADPHONES-001", "AirPods Pro", "249.00"},
            {"KEYBOARD-001", "Magic Keyboard", "299.00"},
            {"MOUSE-001", "Magic Mouse", "99.00"},
            {"MONITOR-001", "Studio Display", "1599.00"},
            {"CASE-001", "Laptop Case", "79.00"},
            {"CHARGER-001", "MagSafe Charger", "39.00"}
    };
    
    private static final String[] SAMPLE_ADDRESSES = {
            "123 Tech Park, Suite 100, San Francisco, CA 94105",
            "456 Innovation Drive, Austin, TX 78701",
            "789 Startup Lane, Seattle, WA 98101",
            "321 Digital Avenue, New York, NY 10001",
            "654 Cloud Street, Boston, MA 02101"
    };
    
    public OrderPlacementScheduler(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }
    
    /**
     * Scheduled task that runs every 2 seconds.
     * Creates a cart, adds items, and places an order.
     */
    @Scheduled(fixedRate = 2000)
    public void placeScheduledOrder() {
        if (!enabled) {
            return;
        }
        
        // Set up MDC for logging correlation
        String correlationId = "scheduled-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
        
        try {
            long orderNum = orderCounter.incrementAndGet();
            log.info("Starting scheduled order placement #{}", orderNum);
            
            // Step 1: Create a cart
            String userId = "scheduled-user-" + orderNum;
            Cart cart = cartService.createCart(new CreateCartRequest(userId));
            log.debug("Created cart: cartId={}", cart.getCartId());
            
            // Step 2: Add random items (1-3 items)
            int itemCount = random.nextInt(3) + 1;
            for (int i = 0; i < itemCount; i++) {
                String[] product = SAMPLE_PRODUCTS[random.nextInt(SAMPLE_PRODUCTS.length)];
                int quantity = random.nextInt(3) + 1;
                
                AddItemRequest itemRequest = new AddItemRequest(
                        product[0],
                        product[1],
                        quantity,
                        new BigDecimal(product[2])
                );
                
                cartService.addItem(cart.getCartId(), itemRequest);
                log.debug("Added item: sku={}, quantity={}", product[0], quantity);
            }
            
            // Step 3: Place the order
            String idempotencyKey = "scheduled-order-" + orderNum + "-" + System.currentTimeMillis();
            String shippingAddress = SAMPLE_ADDRESSES[random.nextInt(SAMPLE_ADDRESSES.length)];
            
            PlaceOrderRequest orderRequest = new PlaceOrderRequest(
                    cart.getCartId(),
                    idempotencyKey,
                    shippingAddress,
                    "Scheduled order #" + orderNum
            );
            
            OrderService.OrderResult result = orderService.placeOrder(orderRequest);
            Order order = result.order();
            
            log.info("Scheduled order #{} placed successfully: orderId={}, items={}, total={}", 
                    orderNum, order.getOrderId(), order.getItemCount(), order.getTotal());
            
        } catch (Exception e) {
            log.error("Failed to place scheduled order: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Returns the total number of scheduled orders placed.
     */
    public long getOrderCount() {
        return orderCounter.get();
    }
}
