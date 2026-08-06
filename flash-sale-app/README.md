# Flash Sale Service

A standalone Spring Boot 4 / Java 25 service implementing two user stories:

- **User Story 1**: customers can purchase products during flash sales that have
  limited stock and a fixed start/end time window.
- **User Story 2**: premium customers get priority access to flash sales, based on
  membership level and purchase history.

It reuses the same architectural patterns as the earlier `ecom-app` kata (Postgres +
Flyway, a transactional outbox publishing to Kafka, `BusinessException`/`ErrorType`/
`GlobalExceptionHandler` error handling, Testcontainers + embedded-Kafka integration
tests) but is a fully independent project - own database, own Kafka broker, own
`docker-compose.yml`.

## Architecture

```text
Client -> POST /api/flash-sales/{id}/purchase-requests -> PENDING request persisted
                                                                   |
                                    @Scheduled FlashSalePurchaseProcessor (every 500ms)
                                                                   |
                        FlashSaleAllocationService: rank pending requests, then
                        award remaining stock in that order until it runs out
                                                                   |
                        CONFIRMED -> Purchase row + outbox event -> Kafka
                        (rest)    -> REJECTED_SOLD_OUT / REJECTED_SALE_ENDED
```

Purchases are processed in scheduled batches rather than one at a time as HTTP
requests race in. That is what makes "priority" meaningful: when a batch has more
demand than remaining stock, requests are ranked before any stock is awarded, instead
of handing it out strictly in arrival order.

### Priority ranking

When a batch of pending requests is drained, they are ordered by:

1. Membership tier, highest first (`PLATINUM > GOLD > SILVER > STANDARD`)
2. Purchase history (`purchaseCount`), highest first, as a tiebreaker within a tier
3. Request timestamp, earliest first, as the final tiebreaker

### Early access

A flash sale has an `earlyAccessStart` timestamp before its public `startTime`.
Between the two, only customers at/above `flash-sale.priority.early-access-min-level`
(default `GOLD`, configurable in `application.yaml`) may submit a purchase request; an
unregistered `customerId` is treated as `STANDARD` and denied. From `startTime` onward,
anyone may submit - stock contention is then resolved purely by the ranking above.

Purchase history accrues automatically: a customer's `purchaseCount` increments on
every confirmed purchase, even if they were never explicitly registered. Only the
membership *tier* itself has to be granted out-of-band, via the customer API.

## Running locally

```bash
cd flash-sale-app
docker compose up --build
```

| Service             | URL                                          |
|----------------------|-----------------------------------------------|
| flash-sale-service    | http://localhost:8090                         |
| Swagger UI            | http://localhost:8090/swagger-ui/index.html   |
| Postgres               | localhost:5436 (`flash_sale_db`)              |
| Redpanda (Kafka)       | localhost:9093                                |

Flyway seeds 3 demo products and one customer per membership tier
(`cust-standard`, `cust-silver`, `cust-gold`, `cust-platinum`) on startup.

## API

| Method | Endpoint                                              | Description                                   |
|--------|--------------------------------------------------------|------------------------------------------------|
| GET    | `/api/products`                                         | List products available to put on sale         |
| POST   | `/api/flash-sales`                                       | Create a flash sale                             |
| GET    | `/api/flash-sales`                                       | List all flash sales                            |
| GET    | `/api/flash-sales/{id}`                                  | Get a flash sale                                |
| POST   | `/api/flash-sales/{id}/purchase-requests`                | Submit a purchase request (returns 202 + id)    |
| GET    | `/api/flash-sales/{id}/purchase-requests/{requestId}`    | Poll a purchase request's outcome               |
| POST   | `/api/customers`                                          | Register a customer / update their tier         |
| GET    | `/api/customers/{id}`                                     | Get a customer's profile and purchase history   |

## Tests

```bash
cd flash-sale-app/flash-sale-service
mvn test
```

- `src/test/.../unitaires/` - pure unit tests (window/stock boundaries, ranking and
  allocation logic, validation), no Spring context.
- `src/test/.../integration/` - full-stack tests using Testcontainers Postgres, an
  embedded Kafka broker, and a real HTTP client, covering both user stories end to end
  (including a real stock-contention scenario proving a premium customer outranks a
  standard one for the last unit of stock).

## Branching

This project follows a simplified Git Flow: `develop` is the integration branch, and
each user story was implemented on its own `feature/user_story_*` branch before being
merged back into `develop`.
