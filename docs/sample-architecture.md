# Architecture: Web shop

A web app calls a Spring Boot order service that stores orders in PostgreSQL and refunds through the payment gateway.

## System context

```mermaid
flowchart TB
    nA1["Customer<br/>[Person]<br/>Buys and cancels orders"]
    system["Web shop<br/>[Software System]"]
    nX1["Payment gateway<br/>[External System]<br/>Card payments and refunds"]
    nA1 -->|"Cancels orders<br/>[HTTPS]"| system
    system -->|"Issues refunds<br/>[HTTPS]"| nX1
```

## Containers

```mermaid
flowchart TB
    nA1["Customer<br/>[Person]<br/>Buys and cancels orders"]
    subgraph system ["Web shop"]
        nC1["Web app<br/>[Container: React]<br/>Order pages for customers"]
        nC2["Order service<br/>[Container: Spring Boot]<br/>Cancels orders and issues refunds"]
        nC3["Order database<br/>[Container: PostgreSQL]<br/>Stores orders and refunds"]
    end
    nX1["Payment gateway<br/>[External System]<br/>Card payments and refunds"]
    nA1 -->|"Cancels orders<br/>[HTTPS]"| nC1
    nC1 -->|"Calls the order API<br/>[HTTPS/JSON]"| nC2
    nC2 -->|"Reads and writes orders<br/>[JDBC]"| nC3
    nC2 -->|"Issues refunds<br/>[HTTPS]"| nX1
```

| Id | Container | Technology | Responsibility | Requirements |
|----|-----------|------------|----------------|--------------|
| C-1 | Web app | React | Order pages for customers | US-1 |
| C-2 | Order service | Spring Boot | Cancels orders and issues refunds | US-1, US-2, NFR-1 |
| C-3 | Order database | PostgreSQL | Stores orders and refunds | US-1, US-2 |

## Key flows

### US-1: Cancel an unshipped order

```mermaid
sequenceDiagram
    actor nA1 as Customer
    participant nC1 as Web app
    participant nC2 as Order service
    participant nC3 as Order database
    nA1->>nC1: Cancel order
    nC1->>nC2: POST /orders/{id}/cancel
    nC2->>nC3: Set status to Cancelled
```

## Domain model

```mermaid
erDiagram
    Order {
        UUID id
        OrderStatus status
    }
    Refund {
        UUID id
        Money amount
    }
    Order ||--o{ Refund : "is refunded by"
```

## Deployment: AWS EKS

```mermaid
flowchart TB
    subgraph nD1 ["EKS cluster [Kubernetes]"]
        nD1_nC1["Web app<br/>[React]"]
        nD1_nC2["Order service<br/>[Spring Boot]"]
    end
    subgraph nD2 ["Amazon RDS [PostgreSQL 16]"]
        nD2_nC3["Order database<br/>[PostgreSQL]"]
    end
```

## Architecture decisions

### ADR-1: Issue refunds asynchronously

- **Status:** Proposed
- **Requirements:** US-2, NFR-1

**Context:** The payment gateway can be slow or unavailable, and refunds must arrive within 5 business days.

**Decision:** The order service records a pending refund and a scheduled job sends it to the payment gateway, retrying on failure.

**Alternatives considered:**

- Refund synchronously during cancellation

**Consequences:** Cancellation stays fast; refunds need monitoring.

## NFR traceability

| NFR | Category | Statement | Tactic | Containers |
|-----|----------|-----------|--------|------------|
| NFR-1 | PERFORMANCE | Refunds reach the original payment method within 5 business days. | Retry failed refunds with a scheduled job and alert after 3 days | Order service |

## Gaps

- **Stories without a flow:** US-2

## Assumptions

- Refunds are only issued to cards

## Open questions

- Who is alerted when a refund keeps failing?
