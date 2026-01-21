package com.telekom.orderPlacement.api.controller;

import com.telekom.orderPlacement.repository.CartRepository;
import com.telekom.orderPlacement.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health and status controller for operational visibility.
 * 
 * Provides endpoints for:
 * - Basic health check
 * - System statistics
 * - Readiness probe for Kubernetes
 */
@RestController
@RequestMapping
public class HealthController {
    
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    
    public HealthController(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "order-placement-service"
        ));
    }
    
    /**
     * System statistics endpoint.
     * Provides counts of carts and orders in the system.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "activeCarts", cartRepository.count(),
                "totalOrders", orderRepository.count(),
                "timestamp", Instant.now().toString()
        ));
    }
}
