package com.telekom.orderPlacement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Placement System - Main Application Entry Point
 * 
 * =============================================================================
 * PRODUCTION-READY ORDER PLACEMENT SYSTEM
 * =============================================================================
 * 
 * This application implements a complete cart and order management system with:
 * - RESTful APIs for cart and order operations
 * - In-memory storage with thread-safe data structures
 * - Kafka integration for event streaming and logging
 * - JSON-formatted logs for Elasticsearch/Kibana observability
 * - Correlation ID propagation for distributed tracing
 * - Idempotent order placement for reliability
 * 
 * Architecture:
 * - Controller Layer: REST API endpoints
 * - Service Layer: Business logic and validation
 * - Repository Layer: In-memory data storage
 * - Kafka Layer: Event publishing
 * - Logging Layer: Structured logging with MDC
 * 
 * Quick Start:
 * 1. Start Kafka (optional, can run without): docker-compose up -d
 * 2. Run application: ./mvnw spring-boot:run
 * 3. Test API: curl http://localhost:8080/actuator/health
 * 
 * API Documentation:
 * - POST /cart - Create cart
 * - POST /cart/{id}/items - Add item
 * - POST /orders/place - Place order
 * - See README.md for complete API reference
 * 
 * @author Order Placement Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class OrderPlacementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPlacementApplication.class, args);
    }

}
