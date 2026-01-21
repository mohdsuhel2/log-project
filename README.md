# Order Placement System

A production-ready Order Placement System built with Java 17, Spring Boot 3.2, and Apache Kafka, featuring comprehensive observability through structured logging and Kibana integration.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [API Reference](#api-reference)
5. [Data Models](#data-models)
6. [Kafka Events](#kafka-events)
7. [Observability](#observability)
8. [Curl Examples](#curl-examples)
9. [Kibana Queries](#kibana-queries)
10. [Configuration](#configuration)
11. [Design Decisions](#design-decisions)

---

## Overview

### Features

- **Cart Management**: Create, update, and manage shopping carts
- **Order Placement**: Idempotent order creation with exactly-once semantics
- **Event Streaming**: Kafka-based event publishing for all operations
- **Structured Logging**: JSON-formatted logs with correlation ID propagation
- **Observability**: Full integration with Elasticsearch and Kibana
- **Thread-Safe**: Concurrent operations with optimistic locking
- **Production-Ready**: Comprehensive error handling and validation

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Event Streaming | Apache Kafka |
| Logging | Logback + Logstash Encoder |
| Metrics | Micrometer + Prometheus |
| Search/Analytics | Elasticsearch + Kibana |
| Build Tool | Maven |

---

## Architecture

### Package Structure

```
com.telekom.orderPlacement/
├── api/
│   ├── controller/      # REST controllers
│   └── dto/
│       ├── request/     # Request DTOs with validation
│       └── response/    # Response DTOs
├── config/              # Configuration classes
├── domain/
│   └── model/           # Domain entities (Cart, Order, etc.)
├── exception/           # Custom exceptions and global handler
├── kafka/
│   ├── event/           # Event models and types
│   │   └── payload/     # Event payloads
│   └── producer/        # Kafka producer
├── logging/             # Correlation ID and request logging
├── repository/          # In-memory repositories
└── service/             # Business logic
```

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer                               │
│   Controllers, DTOs, Validation, Exception Handling          │
├─────────────────────────────────────────────────────────────┤
│                   Service Layer                              │
│   Business Logic, Orchestration, Event Publishing            │
├─────────────────────────────────────────────────────────────┤
│                  Domain Layer                                │
│   Entities, Value Objects, Business Rules                    │
├─────────────────────────────────────────────────────────────┤
│               Infrastructure Layer                           │
│   Repositories, Kafka, Logging, Configuration                │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Client Request → Controller → Service → Repository (In-Memory)
                    │                        │
                    ▼                        │
              Kafka Producer ◄───────────────┘
                    │
                    ▼
              Kafka Topics
                    │
                    ▼
    Logstash → Elasticsearch → Kibana
```

---

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for Kafka/Elasticsearch)

### Running Locally

1. **Start Infrastructure** (Kafka, Elasticsearch, Kibana):
```bash
docker-compose up -d
```

2. **Wait for services to be healthy**:
```bash
docker-compose ps
```

3. **Start the Application**:
```bash
./mvnw spring-boot:run
```

4. **Verify the application is running**:
```bash
curl http://localhost:8080/actuator/health
```

### Running Without Kafka

The application can run without Kafka by setting:
```bash
KAFKA_ENABLED=false ./mvnw spring-boot:run
```

---

## API Reference

### Cart APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/cart` | Create a new cart |
| `GET` | `/cart/{cartId}` | Get cart details |
| `POST` | `/cart/{cartId}/items` | Add item to cart |
| `PUT` | `/cart/{cartId}/items/{itemId}` | Update item quantity |
| `DELETE` | `/cart/{cartId}/items/{itemId}` | Remove item from cart |
| `DELETE` | `/cart/{cartId}/items` | Clear all items |
| `DELETE` | `/cart/{cartId}` | Delete cart |

### Order APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/orders/place` | Place order (idempotent) |
| `GET` | `/orders/{orderId}` | Get order details |
| `GET` | `/orders?userId={id}` | List user's orders |
| `GET` | `/orders?status={status}` | List orders by status |
| `POST` | `/orders/{orderId}/confirm` | Confirm order |
| `POST` | `/orders/{orderId}/cancel` | Cancel order |
| `PUT` | `/orders/{orderId}/status?newStatus={status}` | Update status |

### HTTP Headers

| Header | Description |
|--------|-------------|
| `X-Correlation-ID` | Request correlation ID (auto-generated if not provided) |
| `Content-Type` | Must be `application/json` |

### Response Format

All responses follow this structure:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "correlationId": "abc-123-def-456",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "error": null
}
```

Error response:

```json
{
  "success": false,
  "message": "Cart not found",
  "data": null,
  "correlationId": "abc-123-def-456",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "error": {
    "code": "CART_NOT_FOUND",
    "description": "Cart not found: cart-123",
    "path": "/cart/cart-123",
    "fieldErrors": null
  }
}
```

---

## Data Models

### Cart

```json
{
  "cartId": "uuid",
  "userId": "user-123",
  "items": [
    {
      "itemId": "uuid",
      "sku": "PROD-001",
      "productName": "Wireless Mouse",
      "quantity": 2,
      "unitPrice": 29.99,
      "totalPrice": 59.98,
      "addedAt": "2024-01-15T10:00:00Z",
      "updatedAt": "2024-01-15T10:05:00Z"
    }
  ],
  "itemCount": 2,
  "totalPrice": 59.98,
  "createdAt": "2024-01-15T09:00:00Z",
  "updatedAt": "2024-01-15T10:05:00Z",
  "version": 3
}
```

### Order

```json
{
  "orderId": "uuid",
  "userId": "user-123",
  "cartId": "uuid",
  "status": "PENDING",
  "statusDescription": "Order is pending confirmation",
  "items": [...],
  "itemCount": 2,
  "subtotal": 59.98,
  "tax": 6.00,
  "total": 65.98,
  "shippingAddress": "123 Main St, City, Country",
  "notes": "Leave at door",
  "createdAt": "2024-01-15T11:00:00Z",
  "updatedAt": "2024-01-15T11:00:00Z"
}
```

### Order Status Lifecycle

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓          ↓
    └──────────┴──→ CANCELLED
```

---

## Kafka Events

### Topics

| Topic | Description | Retention |
|-------|-------------|-----------|
| `order-system.cart-events` | Cart lifecycle events | 7 days |
| `order-system.order-events` | Order lifecycle events | 30 days |
| `order-system.system-events` | Error/system events | 14 days |

### Event Types

**Cart Events:**
- `CART_CREATED` - New cart created
- `CART_UPDATED` - Cart modified
- `CART_CLEARED` - Cart cleared (order placed)
- `ITEM_ADDED` - Item added to cart
- `ITEM_UPDATED` - Item quantity changed
- `ITEM_REMOVED` - Item removed from cart

**Order Events:**
- `ORDER_PLACED` - New order created
- `ORDER_CONFIRMED` - Order confirmed
- `ORDER_PROCESSING` - Order being processed
- `ORDER_SHIPPED` - Order shipped
- `ORDER_DELIVERED` - Order delivered
- `ORDER_CANCELLED` - Order cancelled

### Event Schema

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_PLACED",
  "correlationId": "request-correlation-id",
  "timestamp": "2024-01-15T11:00:00.000Z",
  "source": "order-placement-service",
  "version": "1.0",
  "payload": {
    "orderId": "uuid",
    "userId": "user-123",
    "total": 65.98,
    "status": "PENDING",
    ...
  }
}
```

### Kafka Headers

| Header | Description |
|--------|-------------|
| `X-Correlation-ID` | Request correlation ID |
| `X-Event-Type` | Event type code |
| `X-Timestamp` | Event timestamp |
| `X-Source` | Source service |
| `X-Schema-Version` | Event schema version |
| `X-Event-ID` | Unique event ID |

---

## Observability

### Log Structure (JSON)

```json
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "@version": "1",
  "message": "Order placed successfully: orderId=abc-123",
  "logger_name": "com.telekom.orderPlacement.service.OrderService",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "correlationId": "abc-123-def-456",
  "requestId": "xyz-789",
  "method": "POST",
  "path": "/orders/place",
  "status": "201",
  "durationMs": "45",
  "application": "order-placement-service"
}
```

### MDC (Mapped Diagnostic Context) Fields

| Field | Description |
|-------|-------------|
| `correlationId` | Unique ID for request tracing |
| `requestId` | Per-request unique ID |
| `method` | HTTP method |
| `path` | Request path |
| `status` | Response status code |
| `durationMs` | Request duration |

### Metrics Endpoints

- `/actuator/health` - Health check
- `/actuator/metrics` - Available metrics
- `/actuator/prometheus` - Prometheus format

---

## Curl Examples

### 1. Create a Cart

```bash
curl -X POST http://localhost:8080/cart \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-corr-001" \
  -d '{
    "userId": "user-123"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Cart created successfully",
  "data": {
    "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userId": "user-123",
    "items": [],
    "itemCount": 0,
    "totalPrice": 0,
    "version": 0
  },
  "correlationId": "test-corr-001"
}
```

### 2. Add Item to Cart

```bash
CART_ID="a1b2c3d4-e5f6-7890-abcd-ef1234567890"

curl -X POST "http://localhost:8080/cart/${CART_ID}/items" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-corr-002" \
  -d '{
    "sku": "MOUSE-001",
    "productName": "Wireless Gaming Mouse",
    "quantity": 2,
    "unitPrice": 49.99
  }'
```

### 3. Add Another Item

```bash
curl -X POST "http://localhost:8080/cart/${CART_ID}/items" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "KEYBOARD-001",
    "productName": "Mechanical Keyboard",
    "quantity": 1,
    "unitPrice": 129.99
  }'
```

### 4. Get Cart Details

```bash
curl -X GET "http://localhost:8080/cart/${CART_ID}" \
  -H "X-Correlation-ID: test-corr-003"
```

### 5. Update Item Quantity

```bash
ITEM_ID="item-uuid-here"

curl -X PUT "http://localhost:8080/cart/${CART_ID}/items/${ITEM_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

### 6. Remove Item from Cart

```bash
curl -X DELETE "http://localhost:8080/cart/${CART_ID}/items/${ITEM_ID}"
```

### 7. Place Order (Idempotent)

```bash
IDEMPOTENCY_KEY=$(uuidgen)

curl -X POST http://localhost:8080/orders/place \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: order-corr-001" \
  -d "{
    \"cartId\": \"${CART_ID}\",
    \"idempotencyKey\": \"${IDEMPOTENCY_KEY}\",
    \"shippingAddress\": \"123 Main Street, Apartment 4B, New York, NY 10001\",
    \"notes\": \"Please leave package at the door\"
  }"
```

Response (201 Created):
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "orderId": "order-uuid",
    "userId": "user-123",
    "status": "PENDING",
    "statusDescription": "Order is pending confirmation",
    "items": [...],
    "subtotal": 229.97,
    "tax": 23.00,
    "total": 252.97
  }
}
```

### 8. Retry Same Order (Idempotent - Returns Existing)

```bash
# Using same IDEMPOTENCY_KEY returns 200 OK with existing order
curl -X POST http://localhost:8080/orders/place \
  -H "Content-Type: application/json" \
  -d "{
    \"cartId\": \"${CART_ID}\",
    \"idempotencyKey\": \"${IDEMPOTENCY_KEY}\",
    \"shippingAddress\": \"123 Main Street, Apartment 4B, New York, NY 10001\"
  }"
```

Response (200 OK - Idempotent):
```json
{
  "success": true,
  "message": "Order already exists (idempotent response)",
  "data": { ... existing order ... }
}
```

### 9. Get Order Details

```bash
ORDER_ID="order-uuid"

curl -X GET "http://localhost:8080/orders/${ORDER_ID}"
```

### 10. Confirm Order

```bash
curl -X POST "http://localhost:8080/orders/${ORDER_ID}/confirm"
```

### 11. Cancel Order

```bash
curl -X POST "http://localhost:8080/orders/${ORDER_ID}/cancel"
```

### 12. List User's Orders

```bash
curl -X GET "http://localhost:8080/orders?userId=user-123"
```

### 13. Complete Order Flow Script

```bash
#!/bin/bash
# Complete order flow demonstration

BASE_URL="http://localhost:8080"
USER_ID="demo-user-$(date +%s)"
CORR_ID="demo-$(uuidgen)"

echo "=== Creating Cart ==="
CART_RESPONSE=$(curl -s -X POST "${BASE_URL}/cart" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: ${CORR_ID}" \
  -d "{\"userId\": \"${USER_ID}\"}")

CART_ID=$(echo $CART_RESPONSE | jq -r '.data.cartId')
echo "Cart ID: ${CART_ID}"

echo -e "\n=== Adding Items ==="
curl -s -X POST "${BASE_URL}/cart/${CART_ID}/items" \
  -H "Content-Type: application/json" \
  -d '{"sku": "LAPTOP-001", "productName": "MacBook Pro 16", "quantity": 1, "unitPrice": 2499.00}'

curl -s -X POST "${BASE_URL}/cart/${CART_ID}/items" \
  -H "Content-Type: application/json" \
  -d '{"sku": "CASE-001", "productName": "Laptop Case", "quantity": 1, "unitPrice": 79.99}'

echo -e "\n=== Cart Contents ==="
curl -s "${BASE_URL}/cart/${CART_ID}" | jq '.data | {items: .items, total: .totalPrice}'

echo -e "\n=== Placing Order ==="
IDEMP_KEY=$(uuidgen)
ORDER_RESPONSE=$(curl -s -X POST "${BASE_URL}/orders/place" \
  -H "Content-Type: application/json" \
  -d "{
    \"cartId\": \"${CART_ID}\",
    \"idempotencyKey\": \"${IDEMP_KEY}\",
    \"shippingAddress\": \"456 Tech Park, Suite 100, San Francisco, CA 94105\"
  }")

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.orderId')
echo "Order ID: ${ORDER_ID}"
echo $ORDER_RESPONSE | jq '.data | {orderId, status, total}'

echo -e "\n=== Confirming Order ==="
curl -s -X POST "${BASE_URL}/orders/${ORDER_ID}/confirm" | jq '.data.status'

echo -e "\n=== Order Complete! ==="
```

---

## Kibana Queries

### Setup

1. Access Kibana: http://localhost:5601
2. Create Index Pattern: `order-system-events-*`
3. Set Time field: `@timestamp`

### Example Queries

#### 1. Track a Single Request (Correlation ID)

```
correlationId: "abc-123-def-456"
```

This shows all logs and events for a single request across the system.

#### 2. All Order Placements

```
eventType.code: "order.placed"
```

#### 3. Failed Operations (Errors)

```
level: ERROR OR eventType.code: "system.error"
```

#### 4. Cart Events for a User

```
event_category: "cart" AND payload_userId: "user-123"
```

#### 5. Orders by Status

```
eventType.code: "order.*" AND payload_status: "CONFIRMED"
```

#### 6. High-Value Orders

```
eventType.code: "order.placed" AND payload_total: >100
```

#### 7. Slow Requests (Latency Analysis)

```
durationMs: >1000
```

For HTTP request logs in application logs index:

```
logger_name: "RequestLoggingFilter" AND durationMs: >500
```

#### 8. Recent Errors with Stack Traces

```
level: ERROR AND _exists_: stackTrace
```

#### 9. Cart Abandonment Analysis

```
eventType.code: "cart.created" AND NOT eventType.code: "order.placed"
```

#### 10. Order Timeline for Specific Order

```
payload_orderId: "order-uuid-here"
```

Sort by `@timestamp` ascending to see the order lifecycle.

### Dashboard Ideas

#### Order Analytics Dashboard

**Visualizations:**
1. **Order Volume Over Time** - Line chart of orders per hour/day
2. **Order Status Distribution** - Pie chart of current order statuses
3. **Average Order Value** - Metric showing avg order total
4. **Top Products** - Table of most ordered SKUs
5. **Order Success Rate** - Gauge showing placed vs failed

**KQL Queries for Each:**

```
# Orders per day
eventType.code: "order.placed"
# Aggregate by @timestamp with date histogram

# Status distribution
eventType.code: "order.*"
# Aggregate by payload_status

# Failed orders
eventType.code: "order.failed" OR (level: ERROR AND path: "/orders/*")
```

#### Cart Analytics Dashboard

**Visualizations:**
1. **Cart Creation Rate** - Line chart over time
2. **Items per Cart Distribution** - Histogram
3. **Cart Conversion Rate** - orders placed / carts created
4. **Popular Products** - Added to cart frequency
5. **Cart Abandonment** - Carts not converted to orders

#### System Health Dashboard

**Visualizations:**
1. **Error Rate** - Errors per minute
2. **Response Time Percentiles** - P50, P95, P99 latency
3. **Request Volume** - Requests per second
4. **Error Types** - Table of error codes
5. **Kafka Lag** - Consumer lag (if available)

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Application port |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `KAFKA_ENABLED` | true | Enable/disable Kafka |
| `KAFKA_PARTITIONS` | 6 | Topic partitions |
| `KAFKA_REPLICATION_FACTOR` | 1 | Topic replication |
| `LOG_LEVEL` | INFO | Root log level |
| `LOG_PATH` | ./logs | Log file path |

### Profiles

- `local` (default): Development settings
- `prod`: Production settings with stricter logging
- `test`: Test settings with Kafka disabled

Running with profile:
```bash
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

---

## Design Decisions

### Why ConcurrentHashMap for In-Memory Storage?

**Chosen Strategy:**
- `ConcurrentHashMap<String, Cart>` for cart storage
- `ConcurrentHashMap<String, Order>` for order storage

**Thread Safety Guarantees:**
- Atomic `compute()` and `computeIfAbsent()` operations
- Lock-free reads for high read throughput
- Segmented locking for writes (16 segments by default)
- No external synchronization needed

**Trade-offs vs Persistent Storage:**

| Aspect | In-Memory | Persistent (e.g., PostgreSQL) |
|--------|-----------|-------------------------------|
| Latency | Sub-microsecond | Milliseconds |
| Durability | Data loss on restart | ACID guarantees |
| Scalability | Limited by heap | Horizontal scaling possible |
| Complexity | Simple | Requires connection pools, ORM |
| Recovery | Lost on crash | Transaction logs, backups |

**Mitigation:**
- Kafka events provide audit trail for recovery
- Could add periodic snapshots to file/S3
- Easy to swap repository implementation for production

### Why Idempotency Key for Orders?

**Problem:** Network failures can cause clients to retry requests, potentially creating duplicate orders.

**Solution:** Client-provided idempotency key ensures exactly-once semantics:

```java
// Atomic check-and-insert
String existingOrderId = idempotencyKeyMap.putIfAbsent(key, newOrderId);
if (existingOrderId != null) {
    return orderMap.get(existingOrderId); // Return existing order
}
```

**How it works:**
1. Client generates unique key (UUID recommended)
2. First request with key creates order, stores key → orderId mapping
3. Retries with same key return existing order (200 OK instead of 201 Created)
4. Different key creates new order

### Why Immutable Order Model?

**Design:** Orders are immutable after creation; status changes create new instances.

```java
public Order withStatus(OrderStatus newStatus) {
    return new Builder()
        .orderId(this.orderId)
        .status(newStatus)
        .updatedAt(Instant.now())
        // ... copy all other fields
        .build();
}
```

**Benefits:**
- Thread-safe without synchronization
- Easy to reason about state
- Audit trail integrity
- Prevents accidental modifications

### Why JSON Logging to Kafka?

**Architecture:** Application → JSON Logs → Kafka → Logstash → Elasticsearch → Kibana

**Benefits:**
- Structured data for querying
- Correlation ID enables distributed tracing
- Decoupled log shipping (app doesn't wait for Elasticsearch)
- Kafka provides durability and replay capability
- Scales to high log volumes

---

## Troubleshooting

### Common Issues

**1. Kafka Connection Failed**
```
Error: Connection refused to localhost:9092
Solution: Start Kafka with docker-compose up -d kafka
Or disable Kafka: KAFKA_ENABLED=false ./mvnw spring-boot:run
```

**2. Port Already in Use**
```
Error: Port 8080 already in use
Solution: SERVER_PORT=8081 ./mvnw spring-boot:run
```

**3. Cart Not Found After Restart**
```
Expected: In-memory storage is cleared on restart
Solution: Re-create cart or use persistent storage in production
```

---

## License

This project is created for demonstration and educational purposes.

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a pull request

---

*Built with ❤️ for production-grade order management*
