# Payment Orchestration & Routing Platform — Product Requirements Document (PRD)

| Field | Value |
|---|---|
| **Document Version** | 1.0 |
| **Status** | Draft → Approved |
| **Author** | Yash |
| **Last Updated** | 2026-01-15 |
| **Project Codename** | `porp` (Payment Orchestration & Routing Platform) |
| **Target Release** | v1.0 (Portfolio GA) |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals & Non-Goals](#3-goals--non-goals)
4. [Personas & Users](#4-personas--users)
5. [Scope & Phases](#5-scope--phases)
6. [Functional Requirements](#6-functional-requirements)
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [System Architecture](#8-system-architecture)
9. [Domain Model](#9-domain-model)
10. [API Specification](#10-api-specification)
11. [Data Model](#11-data-model)
12. [Event Model](#12-event-model)
13. [Security Requirements](#13-security-requirements)
14. [Observability Requirements](#14-observability-requirements)
15. [Testing Strategy](#15-testing-strategy)
16. [Deployment & DevOps](#16-deployment--devops)
17. [Milestones & Timeline](#17-milestones--timeline)
18. [Success Metrics](#18-success-metrics)
19. [Risks & Mitigations](#19-risks--mitigations)
20. [Future Roadmap](#20-future-roadmap)
21. [Appendices](#21-appendices)

---

## 1. Executive Summary

**Payment Orchestration & Routing Platform (PORP)** is an enterprise-grade backend service that sits between merchants and payment providers. It accepts a single payment request from a merchant, intelligently routes it to the optimal provider based on configurable rules, handles retries and failover across providers, tracks the full payment lifecycle, reconciles settlements nightly, and emits reliable events to downstream systems.

The platform is designed as a **modular monolith** using **Spring Boot 3.x (Java 21)**, exercising advanced Spring technologies — Spring Integration, Spring Batch, Spring State Machine, Spring WebFlux, Spring Kafka, Spring Security, Spring Vault, Spring Modulith — because the domain genuinely requires them.

This project serves two purposes:

1. **Learning vehicle** — a comprehensive, hands-on tour of enterprise Spring Boot.
2. **Portfolio artifact** — demonstrable proof of senior-level backend engineering capability.

**Note:** This is a portfolio project. It uses only sandbox/mock providers and never processes real payments or stores real card data.

---

## 2. Problem Statement

### 2.1 The Problem

Modern merchants accept payments through multiple providers (Stripe, Adyen, PayPal, local acquirers). Managing this directly is painful:

- **Fragmentation** — every provider has its own API, error taxonomy, and settlement format.
- **Fragility** — when one provider is down, payments fail unnecessarily.
- **Suboptimal routing** — merchants lose money by sending payments to the wrong provider.
- **Reconciliation hell** — matching provider settlements to internal records is manual and error-prone.
- **No failover** — a declined payment is a lost sale even if another provider would have approved it.
- **Compliance risk** — every provider integration is a new security and audit surface.

### 2.2 The Solution

A centralized orchestration layer that:

- Presents **one unified API** to merchants.
- Abstracts **many providers** behind a common adapter interface.
- Routes payments using **configurable, data-driven rules**.
- Fails over automatically when a provider is unhealthy or declines retryably.
- Tracks every payment through a **finite state machine**.
- Reconciles provider settlements **nightly, at scale**.
- Emits **reliable events** to downstream systems via the Outbox pattern.
- Enforces **multi-tenancy, OAuth2, and audit** by default.

### 2.3 Why Now (for the portfolio)

This project demonstrates in one coherent system:

- Event-driven architecture
- Enterprise integration patterns
- Distributed system resilience
- Large-scale batch processing
- Real-time high-throughput APIs
- Regulatory-aware design
- Observability and SRE practices

No other single portfolio project covers this breadth with real domain justification.

---

## 3. Goals & Non-Goals

### 3.1 Goals

| ID | Goal | Measure of Success |
|---|---|---|
| G1 | Unified payment API across providers | One endpoint accepts payments; ≥3 providers integrated |
| G2 | Intelligent routing engine | Routing decision <200ms p95; rules are configurable at runtime |
| G3 | Reliable payment lifecycle | Every payment traceable through state machine; zero lost payments |
| G4 | Provider resilience | Automatic failover on retryable errors; circuit breakers per provider |
| G5 | Nightly reconciliation | 1M+ settlement records reconciled in <30 minutes |
| G6 | Reliable event delivery | At-least-once delivery via Outbox; idempotent consumers |
| G7 | Multi-tenancy | Schema-per-tenant isolation; verified with integration tests |
| G8 | Enterprise security | OAuth2, Vault-managed secrets, full audit trail |
| G9 | Observability | Metrics, tracing, dashboards, alerts for all critical paths |
| G10 | Portfolio quality | README, ADRs, blog posts, live demo, OpenAPI spec |

### 3.2 Non-Goals (Explicitly Out of Scope for v1.0)

- ❌ Real money movement (mock/sandbox providers only)
- ❌ PCI-DSS certification or card data storage
- ❌ KYC/AML onboarding (mocked)
- ❌ Fraud detection ML models (rules only)
- ❌ Merchant dashboard UI (API + Swagger UI only)
- ❌ Mobile SDKs
- ❌ Microservices (modular monolith only)
- ❌ Multi-region deployment
- ❌ Chargeback arbitration workflows (basic tracking only)
- ❌ Cryptocurrency payments

---

## 4. Personas & Users

### 4.1 Merchant (Primary External User)

- **Who:** Businesses accepting payments (e-commerce, SaaS, marketplaces).
- **Needs:** Simple API, high success rate, low fees, transparent reporting.
- **Interactions:** Create payments, issue refunds, view transactions, receive webhooks.
- **Auth:** OAuth2 client credentials or API key.

### 4.2 Operations Engineer (Internal User)

- **Who:** Staff operating the platform.
- **Needs:** Provider health visibility, routing rule control, dispute investigation.
- **Interactions:** Admin API, Grafana dashboards, reconciliation reports.
- **Auth:** OAuth2 with `ADMIN` role.

### 4.3 Risk & Compliance Analyst (Internal User)

- **Who:** Ensures regulatory compliance and audits transactions.
- **Needs:** Full audit trail, immutable event log, merchant restriction controls.
- **Interactions:** Audit log search, compliance rule config.
- **Auth:** OAuth2 with `COMPLIANCE` role (read-only + audit).

### 4.4 Provider (External System)

- **Who:** Stripe, Adyen, PayPal, mock bank.
- **Needs:** Correct requests, idempotency keys, timely webhook responses.
- **Interactions:** REST APIs, webhooks, settlement file drops.
- **Auth:** API keys / OAuth2 (managed via Vault).

### 4.5 Downstream Consumer (Internal System)

- **Who:** Notification service, analytics, data warehouse.
- **Needs:** Reliable, ordered, idempotent events.
- **Interactions:** Kafka topics.
- **Auth:** mTLS or SASL.

---

## 5. Scope & Phases

The project is delivered in **six waves**, each producing a working, testable increment.

### Wave 1 — Walking Skeleton (Weeks 1–3)

**Outcome:** Payments flow end-to-end through one mock provider.

- Spring Boot 3.x + Java 21 project
- PostgreSQL + Flyway + Redis + Kafka via docker-compose
- `POST /api/v1/payments` with idempotency
- One mock provider adapter
- Basic payment state machine
- Actuator + OpenAPI
- GitHub Actions CI (build + test)

### Wave 2 — Routing Engine (Weeks 4–6)

**Outcome:** Payments route to different mock providers based on rules.

- Merchant configuration
- Provider registry
- Routing rules (cost, success rate, currency, geography, load balance)
- Provider health tracking
- Circuit breakers (Resilience4j)
- Fallback routing
- Routing decision audit

### Wave 3 — Multi-Provider + Events (Weeks 7–9)

**Outcome:** Payments route across real sandbox providers with event streaming.

- 3 providers: Stripe sandbox, Adyen sandbox, mock bank
- Provider-specific error mapping
- Kafka event bus
- Outbox pattern
- Consumer services (notification, analytics)
- Merchant webhooks

### Wave 4 — Settlement & Reconciliation (Weeks 10–12)

**Outcome:** Nightly reconciliation runs and reports discrepancies.

- Settlement service
- Provider settlement file ingestion
- Spring Batch reconciliation job
- Discrepancy detection & reporting
- Scheduled settlement cycles
- Admin reconciliation API

### Wave 5 — Enterprise Hardening (Weeks 13–15)

**Outcome:** Production-like platform with security, multi-tenancy, observability.

- OAuth2 with Spring Authorization Server
- Multi-tenancy (Hibernate schema-per-tenant)
- Spring Vault for secrets
- Rate limiting (Bucket4j)
- Audit log
- Observability (Prometheus, Grafana, OpenTelemetry)
- Load testing (k6)

### Wave 6 — Polish & Portfolio (Weeks 16–17)

**Outcome:** Portfolio-ready deliverable.

- ADRs (10+)
- Architecture diagrams
- README + runbook
- Blog posts (3–5)
- Live demo deployment
- Postman collection
- 5-minute video walkthrough

---

## 6. Functional Requirements

Requirements are labeled `FR-<module>-<n>`.

### 6.1 Merchant Management (FR-MERCH)

| ID | Requirement | Priority |
|---|---|---|
| FR-MERCH-1 | Create a merchant with name, status, and config | Must |
| FR-MERCH-2 | Enable/disable providers per merchant | Must |
| FR-MERCH-3 | Set provider priority per merchant | Must |
| FR-MERCH-4 | Configure routing strategy per merchant (cost, success, custom) | Must |
| FR-MERCH-5 | Set per-merchant rate limits | Should |
| FR-MERCH-6 | Set per-merchant allowed currencies | Should |
| FR-MERCH-7 | Suspend/activate merchant | Must |

### 6.2 Payment Processing (FR-PAY)

| ID | Requirement | Priority |
|---|---|---|
| FR-PAY-1 | Accept payment with amount, currency, method, customer, metadata | Must |
| FR-PAY-2 | Enforce idempotency via `Idempotency-Key` header | Must |
| FR-PAY-3 | Return payment ID and initial status synchronously | Must |
| FR-PAY-4 | Support asynchronous authorization with webhook callback | Must |
| FR-PAY-5 | Support capture (full and partial) | Must |
| FR-PAY-6 | Support cancel before capture | Must |
| FR-PAY-7 | Support refund (full and partial, multiple per payment) | Must |
| FR-PAY-8 | Expose event history for each payment | Must |
| FR-PAY-9 | Expose provider attempts for each payment | Must |
| FR-PAY-10 | Enforce `amount > 0` and supported currency | Must |
| FR-PAY-11 | Support 3DS challenge flow (mock) | Should |
| FR-PAY-12 | Support payment expiration (TTL) | Should |

### 6.3 Routing Engine (FR-ROUTE)

| ID | Requirement | Priority |
|---|---|---|
| FR-ROUTE-1 | Filter providers by merchant config, currency, method, health | Must |
| FR-ROUTE-2 | Score providers using weighted factors (cost, success, latency, preference) | Must |
| FR-ROUTE-3 | Return ranked list of providers for fallback | Must |
| FR-ROUTE-4 | Complete routing decision in <200ms p95 | Must |
| FR-ROUTE-5 | Record every routing decision with rationale | Must |
| FR-ROUTE-6 | Support rule overrides per merchant | Should |
| FR-ROUTE-7 | Support percentage-based load balancing | Should |
| FR-ROUTE-8 | Support custom rules via configuration (no redeploy) | Should |
| FR-ROUTE-9 | Update routing scores from live metrics | Should |

### 6.4 Provider Integration (FR-PROV)

| ID | Requirement | Priority |
|---|---|---|
| FR-PROV-1 | Implement adapter interface for each provider | Must |
| FR-PROV-2 | Map provider errors to internal taxonomy | Must |
| FR-PROV-3 | Support provider-specific idempotency keys | Must |
| FR-PROV-4 | Track provider health (success rate, latency) | Must |
| FR-PROV-5 | Open circuit breaker on failure threshold | Must |
| FR-PROV-6 | Retry with exponential backoff on retryable errors | Must |
| FR-PROV-7 | Support webhook ingestion per provider | Must |
| FR-PROV-8 | Verify webhook signatures | Must |
| FR-PROV-9 | Support provider maintenance windows | Should |
| FR-PROV-10 | Support provider-specific retry policies | Should |

### 6.5 State Machine (FR-SM)

| ID | Requirement | Priority |
|---|---|---|
| FR-SM-1 | Model payment lifecycle as a finite state machine | Must |
| FR-SM-2 | Persist state transitions as immutable events | Must |
| FR-SM-3 | Support retry transitions on retryable failures | Must |
| FR-SM-4 | Support refund and chargeback transitions | Must |
| FR-SM-5 | Prevent invalid transitions | Must |
| FR-SM-6 | Support state versioning for optimistic concurrency | Must |
| FR-SM-7 | Allow state replay for audit | Should |

### 6.6 Settlement (FR-SETTLE)

| ID | Requirement | Priority |
|---|---|---|
| FR-SETTLE-1 | Ingest provider settlement files (CSV, XML, JSON) | Must |
| FR-SETTLE-2 | Match settlement lines to payments | Must |
| FR-SETTLE-3 | Record expected vs actual amounts and dates | Must |
| FR-SETTLE-4 | Detect discrepancies (amount, date, fee, missing) | Must |
| FR-SETTLE-5 | Emit `PaymentSettled` events | Must |
| FR-SETTLE-6 | Trigger merchant payout on settlement | Should |
| FR-SETTLE-7 | Support multi-currency settlement | Should |

### 6.7 Reconciliation (FR-RECON)

| ID | Requirement | Priority |
|---|---|---|
| FR-RECON-1 | Run nightly reconciliation job | Must |
| FR-RECON-2 | Process 1M+ records in <30 minutes | Must |
| FR-RECON-3 | Categorize discrepancies into types | Must |
| FR-RECON-4 | Auto-resolve trivial discrepancies | Should |
| FR-RECON-5 | Report unresolved discrepancies | Must |
| FR-RECON-6 | Support restart on failure | Must |
| FR-RECON-7 | Support manual re-run | Must |
| FR-RECON-8 | Emit reconciliation summary events | Should |

### 6.8 Events & Webhooks (FR-EVT)

| ID | Requirement | Priority |
|---|---|---|
| FR-EVT-1 | Publish domain events to Kafka | Must |
| FR-EVT-2 | Use Outbox pattern for reliable publishing | Must |
| FR-EVT-3 | Guarantee at-least-once delivery | Must |
| FR-EVT-4 | Support idempotent consumers | Must |
| FR-EVT-5 | Send webhooks to merchants for async events | Must |
| FR-EVT-6 | Sign webhooks (HMAC) | Must |
| FR-EVT-7 | Retry failed webhooks with backoff | Must |
| FR-EVT-8 | Expose webhook delivery log | Should |

### 6.9 Administration (FR-ADMIN)

| ID | Requirement | Priority |
|---|---|---|
| FR-ADMIN-1 | Admin API for merchant management | Must |
| FR-ADMIN-2 | Admin API for routing rule management | Must |
| FR-ADMIN-3 | Admin API for provider enable/disable | Must |
| FR-ADMIN-4 | Audit log search API | Must |
| FR-ADMIN-5 | Reconciliation run API | Must |
| FR-ADMIN-6 | Provider health dashboard API | Should |

### 6.10 Audit (FR-AUDIT)

| ID | Requirement | Priority |
|---|---|---|
| FR-AUDIT-1 | Log every sensitive action (who, what, when, why) | Must |
| FR-AUDIT-2 | Immutable audit records | Must |
| FR-AUDIT-3 | Searchable by merchant, actor, action, time | Must |
| FR-AUDIT-4 | Export audit log | Should |

---

## 7. Non-Functional Requirements

Requirements labeled `NFR-<category>-<n>`.

### 7.1 Performance

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-1 | Routing decision latency | p95 < 200ms |
| NFR-PERF-2 | Payment API latency (sync portion) | p95 < 300ms |
| NFR-PERF-3 | Payment throughput (per instance) | ≥ 1,000 RPS |
| NFR-PERF-4 | Reconciliation throughput | 1M records < 30 min |
| NFR-PERF-5 | Event publishing lag | p95 < 1s |
| NFR-PERF-6 | Webhook delivery | p95 < 5s |

### 7.2 Scalability

| ID | Requirement | Target |
|---|---|---|
| NFR-SCALE-1 | Horizontal scaling of API | Stateless, scale to 10 instances |
| NFR-SCALE-2 | Kafka partitions | ≥ 12 per topic |
| NFR-SCALE-3 | DB connection pool | HikariCP, tuned per instance |
| NFR-SCALE-4 | Batch partitioning | Partition by date range and provider |

### 7.3 Reliability

| ID | Requirement | Target |
|---|---|---|
| NFR-REL-1 | Payment loss rate | 0% |
| NFR-REL-2 | Event delivery | At-least-once |
| NFR-REL-3 | Provider failover | Automatic, <500ms |
| NFR-REL-4 | Service availability | 99.9% (target for demo) |
| NFR-REL-5 | Idempotency | 100% of duplicate requests detected |

### 7.4 Security

| ID | Requirement | Target |
|---|---|---|
| NFR-SEC-1 | OAuth2 for all merchant APIs | Required |
| NFR-SEC-2 | Secrets in Vault | Required |
| NFR-SEC-3 | TLS everywhere | Required |
| NFR-SEC-4 | Webhook signature verification | Required |
| NFR-SEC-5 | Rate limiting | Per merchant + per IP |
| NFR-SEC-6 | PII encryption at rest | Required for identifiable fields |
| NFR-SEC-7 | No card data storage | Enforced |

### 7.5 Observability

| ID | Requirement | Target |
|---|---|---|
| NFR-OBS-1 | Metrics exposed | All critical paths |
| NFR-OBS-2 | Distributed tracing | 100% of payment flows |
| NFR-OBS-3 | Structured logging | JSON with correlation ID |
| NFR-OBS-4 | Dashboards | Provider health, throughput, latency, reconciliation |
| NFR-OBS-5 | Alerting | Success rate, latency, discrepancies, DLQ |

### 7.6 Maintainability

| ID | Requirement | Target |
|---|---|---|
| NFR-MAINT-1 | Code coverage | ≥ 80% domain, ≥ 60% overall |
| NFR-MAINT-2 | Modular boundaries | Enforced by Spring Modulith |
| NFR-MAINT-3 | Static analysis | SonarQube / SpotBugs clean |
| NFR-MAINT-4 | Code style | Spotless + Checkstyle |
| NFR-MAINT-5 | API documentation | OpenAPI 3.1, always current |
| NFR-MAINT-6 | ADRs | One per major decision |

### 7.7 Testability

| ID | Requirement | Target |
|---|---|---|
| NFR-TEST-1 | Unit tests | Domain logic, routing, state machine |
| NFR-TEST-2 | Integration tests | Testcontainers for Postgres, Redis, Kafka |
| NFR-TEST-3 | Contract tests | Spring Cloud Contract per provider |
| NFR-TEST-4 | Batch tests | `@SpringBatchTest` for jobs |
| NFR-TEST-5 | Performance tests | k6 scripts for API and batch |

### 7.8 Portability

| ID | Requirement | Target |
|---|---|---|
| NFR-PORT-1 | Containerized | Docker multi-stage |
| NFR-PORT-2 | Local dev | `docker-compose up` |
| NFR-PORT-3 | Cloud deploy | Fly.io / Railway / AWS ECS |
| NFR-PORT-4 | Config externalized | Env-based profiles |

### 7.9 Compliance & Audit

| ID | Requirement | Target |
|---|---|---|
| NFR-COMP-1 | Immutable audit log | Append-only table |
| NFR-COMP-2 | Data retention | Configurable per table |
| NFR-COMP-3 | Data deletion | GDPR-compatible soft-delete |
| NFR-COMP-4 | PII minimization | Only what's needed |

---

## 8. System Architecture

### 8.1 High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                        Merchants / Clients                         │
└───────────────────────────────┬────────────────────────────────────┘
                                │ HTTPS + OAuth2
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                      Spring Cloud Gateway                          │
│                  (rate limit, auth, routing)                       │
└───────────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                    PORP Application (Modular Monolith)             │
│                                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐     │
│  │ Merchant API │  │  Admin API   │  │   Webhook Receiver   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘     │
│         │                 │                     │                 │
│  ┌──────▼─────────────────▼─────────────────────▼───────────┐     │
│  │              Idempotency + Security Layer                │     │
│  │              (Redis + Spring Security + Vault)           │     │
│  └──────────────────────────┬───────────────────────────────┘     │
│                             │                                     │
│  ┌──────────────────────────▼───────────────────────────────┐     │
│  │                   Routing Engine                         │     │
│  │       (rules, scoring, provider health, fallback)        │     │
│  └──────────────────────────┬───────────────────────────────┘     │
│                             │                                     │
│  ┌──────────────────────────▼───────────────────────────────┐     │
│  │              Payment State Machine                       │     │
│  │    (Spring State Machine + event sourcing + outbox)      │     │
│  └─────┬──────────┬──────────┬──────────┬──────────┬────────┘     │
│        │          │          │          │          │              │
│  ┌─────▼───┐ ┌────▼───┐ ┌────▼───┐ ┌────▼───┐ ┌────▼────┐        │
│  │ Stripe  │ │ Adyen  │ │ PayPal │ │ Mock   │ │  Bank   │        │
│  │ Adapter │ │Adapter │ │Adapter │ │Adapter │ │Adapter  │        │
│  └─────┬───┘ └────┬───┘ └────┬───┘ └────┬───┘ └────┬────┘        │
│        │          │          │          │          │              │
│  ┌─────▼──────────▼──────────▼──────────▼──────────▼────────┐     │
│  │              Spring Integration Flows                    │     │
│  │              (transformation, error mapping)             │     │
│  └──────────────────────────┬───────────────────────────────┘     │
│                             │                                     │
│  ┌──────────────────────────▼───────────────────────────────┐     │
│  │                Event Bus (Kafka) + Outbox                │     │
│  └───┬──────────┬───────────┬───────────┬───────────┬───────┘     │
│      │          │           │           │           │             │
│  ┌───▼───┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼─────┐       │
│  │Notif. │ │Settlement│ │Recon.  │ │Analytics│ │  Audit   │       │
│  │Service│ │ Service │ │ Batch  │ │Consumer │ │ Consumer │       │
│  └───────┘ └─────────┘ └─────────┘ └─────────┘ └──────────┘       │
└────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│  Infrastructure: Postgres │ Redis │ Kafka │ Vault │ Prometheus │   │
│                  Grafana │ Loki │ OpenTelemetry │ Elasticsearch    │
└────────────────────────────────────────────────────────────────────┘
```

### 8.2 Module Structure (Spring Modulith)

```
com.porp
├── merchant         (merchant config, onboarding, provider preferences)
├── payment          (payment API, state machine, lifecycle)
├── routing          (routing engine, rules, scoring)
├── provider         (provider registry, adapters, health)
│   ├── stripe
│   ├── adyen
│   ├── paypal
│   └── mock
├── settlement       (settlement ingestion, matching)
├── reconciliation   (batch reconciliation jobs)
├── event            (outbox, Kafka publishing, event types)
├── webhook          (merchant webhook delivery, retries)
├── idempotency      (Redis + DB idempotency)
├── security         (OAuth2 config, authorization server)
├── tenancy          (multi-tenancy context, resolvers)
├── audit            (audit log, AOP aspects)
├── admin            (admin API)
└── shared           (common types, exceptions, utilities)
```

**Boundaries enforced by Spring Modulith:** no cross-module access to internal classes; communication via public APIs and events.

### 8.3 Deployment Topology (v1.0)

- Single deployable Spring Boot JAR (modular monolith)
- Postgres (one DB, multi-schema)
- Redis (cache + idempotency)
- Kafka (3 brokers locally via docker-compose; managed in cloud)
- Vault (dev mode locally; managed in cloud)
- Prometheus + Grafana + Loki + Tempo (observability stack)

---

## 9. Domain Model

### 9.1 Core Aggregates

#### Merchant (Aggregate Root)

```
Merchant
├── id: UUID
├── name: String
├── status: MerchantStatus { ACTIVE, SUSPENDED, PENDING }
├── routingStrategy: RoutingStrategy
├── allowedCurrencies: Set<Currency>
├── providers: List<MerchantProvider>
└── createdAt, updatedAt
```

#### Payment (Aggregate Root)

```
Payment
├── id: UUID
├── merchantId: UUID
├── idempotencyKey: String
├── amount: Money
├── paymentMethod: PaymentMethod
├── customer: CustomerRef
├── status: PaymentState
├── stateVersion: int
├── provider: ProviderId
├── providerReference: String
├── attempts: List<PaymentAttempt>
├── events: List<PaymentEvent>
└── createdAt, updatedAt
```

#### Settlement (Aggregate Root)

```
Settlement
├── id: UUID
├── paymentId: UUID
├── provider: ProviderId
├── expectedAmount: Money
├── actualAmount: Money
├── expectedDate: LocalDate
├── actualDate: LocalDate
├── status: SettlementStatus
├── discrepancyReason: String?
└── createdAt, updatedAt
```

### 9.2 Value Objects

- `Money` — amount + currency, BigDecimal-based
- `PaymentMethod` — type + brand + last4 + country (no PAN)
- `ProviderId` — enum/string identifier
- `IdempotencyKey` — merchant-scoped
- `RoutingDecision` — ranked providers + rationale

### 9.3 Payment State Machine

```
   RECEIVED
      │
      ▼
   ROUTED ◄──────────────┐
      │                  │
      ▼                  │
  AUTHORIZING ──(retry)──┤
      │                  │
      ├──► AUTHORIZED ───┘
      │        │
      │        ▼
      │     CAPTURING
      │        │
      │        ▼
      │     CAPTURED
      │        │
      │        ▼
      │     SETTLING
      │        │
      │        ▼
      │     SETTLED ──► REFUND_REQUESTED ──► REFUNDED
      │        │
      │        └──► CHARGEBACK_RECEIVED ──► CHARGEBACK_RESOLVED
      │
      ├──► AUTHORIZATION_FAILED (terminal)
      ├──► ROUTING_FAILED (terminal)
      ├──► CAPTURE_FAILED (terminal)
      ├──► CANCELLED (terminal)
      └──► EXPIRED (terminal)
```

Every transition:
- Validated by Spring State Machine
- Persisted in `payment_events` (append-only)
- Emitted to Kafka via Outbox
- Audited

---

## 10. API Specification

Base URL: `https://api.porp.example.com/api/v1`

Auth: `Authorization: Bearer <token>` (OAuth2)

### 10.1 Merchant API

#### Create Payment

```http
POST /payments
Idempotency-Key: merchant_123-order_789
Content-Type: application/json
Authorization: Bearer <token>

{
  "amount": 4999,
  "currency": "USD",
  "paymentMethod": {
    "type": "CARD",
    "token": "tok_visa_4242",
    "country": "US"
  },
  "customer": {
    "id": "cust_456",
    "country": "US"
  },
  "capture": true,
  "metadata": { "orderId": "order_789" }
}
```

**Response 201:**

```json
{
  "id": "pay_01H...",
  "status": "AUTHORIZED",
  "amount": 4999,
  "currency": "USD",
  "provider": "stripe",
  "providerReference": "pi_3N...",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

#### Get Payment

```http
GET /payments/{id}
```

#### List Payments

```http
GET /payments?status=AUTHORIZED&from=2026-01-01&to=2026-01-31&page=0&size=50
```

#### Capture Payment

```http
POST /payments/{id}/capture
{ "amount": 4999 }
```

#### Cancel Payment

```http
POST /payments/{id}/cancel
```

#### Refund Payment

```http
POST /payments/{id}/refund
Idempotency-Key: merchant_123-refund_1
{ "amount": 2000, "reason": "customer_request" }
```

#### Get Payment Events

```http
GET /payments/{id}/events
```

#### Get Payment Attempts

```http
GET /payments/{id}/attempts
```

#### Provider Webhook Receiver

```http
POST /webhooks/providers/{provider}
X-Provider-Signature: <hmac>
```

### 10.2 Admin API

```http
POST   /admin/merchants
PUT    /admin/merchants/{id}/providers
PUT    /admin/merchants/{id}/routing-strategy
POST   /admin/routing-rules
PUT    /admin/providers/{name}/disable
GET    /admin/providers/{name}/health
GET    /admin/audit?merchantId=&actor=&from=&to=
POST   /admin/reconciliation/runs
GET    /admin/reconciliation/runs/{id}
```

### 10.3 Error Format

All errors follow RFC 7807:

```json
{
  "type": "https://docs.porp.example.com/errors/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 402,
  "detail": "The card was declined by the issuer.",
  "instance": "/payments/pay_01H...",
  "code": "INSUFFICIENT_FUNDS",
  "providerError": { "provider": "stripe", "code": "card_declined" }
}
```

### 10.4 Error Taxonomy

| Internal Code | HTTP | Retryable | Notes |
|---|---|---|---|
| `INSUFFICIENT_FUNDS` | 402 | No | Card declined |
| `CARD_EXPIRED` | 402 | No | |
| `FRAUD_SUSPECTED` | 402 | No | |
| `PROVIDER_TIMEOUT` | 504 | Yes | Try next provider |
| `PROVIDER_UNAVAILABLE` | 503 | Yes | |
| `RATE_LIMITED` | 429 | Yes | Back off |
| `NETWORK_ERROR` | 502 | Yes | |
| `INVALID_REQUEST` | 400 | No | Client bug |
| `IDEMPOTENCY_CONFLICT` | 409 | No | Key reuse with different body |
| `UNAUTHORIZED` | 401 | No | |
| `FORBIDDEN` | 403 | No | |

---

## 11. Data Model

### 11.1 Schema (Simplified)

```sql
-- Common (shared across tenants)
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    routing_strategy VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE merchant_providers (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    provider VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    priority INT NOT NULL,
    config JSONB NOT NULL,
    UNIQUE (merchant_id, provider)
);

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,
    merchant_id UUID REFERENCES merchants(id),
    name VARCHAR(255) NOT NULL,
    expression TEXT NOT NULL,
    priority INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Tenant schema (per merchant) — tables below replicated per schema
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    state_version INT NOT NULL,
    provider VARCHAR(50),
    provider_reference VARCHAR(255),
    payment_method JSONB NOT NULL,
    customer JSONB NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (merchant_id, idempotency_key)
);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider ON payments(provider);
CREATE INDEX idx_payments_created_at ON payments(created_at);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50),
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    error_message TEXT,
    latency_ms INT,
    provider_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    expected_amount BIGINT NOT NULL,
    actual_amount BIGINT,
    currency CHAR(3) NOT NULL,
    expected_date DATE NOT NULL,
    actual_date DATE,
    status VARCHAR(50) NOT NULL,
    discrepancy_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_settlements_status ON settlements(status);

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);
CREATE INDEX idx_outbox_unpublished ON outbox(published) WHERE published = FALSE;

CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    merchant_id UUID NOT NULL,
    response JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id UUID NOT NULL,
    before JSONB,
    after JSONB,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_actor ON audit_log(actor);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
```

### 11.2 Multi-Tenancy Strategy

- **Schema-per-tenant** for merchant-scoped tables
- **Shared schema** for merchant registry, routing rules, audit log
- Hibernate `CurrentTenantIdentifierResolver` reads `TenantContext`
- `TenantContext` populated from OAuth2 token claim or `X-Merchant-Id` header (admin only)
- Batch jobs iterate tenants via `TenantRegistry`

### 11.3 Redis Usage

| Key Pattern | Purpose | TTL |
|---|---|---|
| `idem:{merchant}:{key}` | Idempotency response cache | 24h |
| `provider:health:{provider}` | Rolling health metrics | 5m |
| `routing:config:{merchant}` | Cached merchant routing config | 5m |
| `ratelimit:{merchant}:{window}` | Rate limit counter | 60s |
| `provider:circuit:{provider}` | Circuit breaker state | Dynamic |

---

## 12. Event Model

### 12.1 Event Envelope

All events share a common envelope:

```json
{
  "eventId": "evt_01H...",
  "eventType": "PaymentAuthorized",
  "aggregateId": "pay_01H...",
  "aggregateType": "Payment",
  "merchantId": "merchant_123",
  "occurredAt": "2026-01-15T10:30:00Z",
  "correlationId": "corr_xyz",
  "causationId": "cmd_abc",
  "version": 3,
  "payload": { }
}
```

### 12.2 Event Types

| Event | Trigger | Consumers |
|---|---|---|
| `PaymentCreated` | Payment received | Analytics, Audit |
| `PaymentRouted` | Provider selected | Analytics |
| `PaymentAuthorizing` | Request sent to provider | — |
| `PaymentAuthorized` | Provider approved | Notification, Settlement, Analytics |
| `PaymentAuthorizationFailed` | Provider declined | Notification, Analytics |
| `PaymentCaptured` | Funds captured | Settlement, Notification |
| `PaymentCaptureFailed` | Capture error | Notification |
| `PaymentCancelled` | Merchant cancelled | Notification |
| `PaymentSettled` | Settlement recorded | Notification, Payout |
| `PaymentRefunded` | Refund completed | Notification, Settlement |
| `ChargebackReceived` | Provider reported chargeback | Notification, Risk |
| `ChargebackResolved` | Chargeback outcome | Notification |
| `SettlementDiscrepancyDetected` | Reconciliation mismatch | Ops alert |
| `ReconciliationRunCompleted` | Batch job finished | Ops dashboard |

### 12.3 Kafka Topics

| Topic | Partitions | Retention |
|---|---|---|
| `porp.payments.events` | 12 | 7d |
| `porp.settlements.events` | 6 | 30d |
| `porp.reconciliation.events` | 3 | 30d |
| `porp.audit.events` | 6 | 90d |
| `porp.payments.events.dlq` | 3 | 30d |

### 12.4 Outbox Publishing

- Outbox row written **in the same transaction** as state change
- Publisher polls unpublished rows every 500ms (or uses Debezium)
- On successful Kafka publish, marks `published = true`
- Idempotent consumer uses `eventId` for dedup

---

## 13. Security Requirements

### 13.1 Authentication

- **Merchant APIs:** OAuth2 client credentials (machine-to-machine)
- **Admin APIs:** OAuth2 authorization code + PKCE (human users)
- **Webhooks (inbound):** Provider-specific signature verification
- **Webhooks (outbound):** HMAC-SHA256 signature header

### 13.2 Authorization

- Scopes: `payments:read`, `payments:write`, `refunds:write`, `settlements:read`, `admin:*`
- Roles: `MERCHANT`, `ADMIN`, `COMPLIANCE`
- Method security with `@PreAuthorize`
- Tenant isolation enforced at repository layer via Hibernate filter

### 13.3 Secrets

- Provider API keys stored in **HashiCorp Vault**
- App reads via `spring-cloud-vault`
- No secrets in config files or env vars beyond Vault token
- Rotation supported via Vault dynamic secrets (future)

### 13.4 Data Protection

- TLS 1.3 in transit
- PII fields encrypted at rest (Jasypt or DB-level)
- No PAN, CVV, or full card numbers stored
- Tokenized payment methods only
- Audit log immutable (append-only, no updates)

### 13.5 Rate Limiting

- Per merchant: configurable RPS + burst
- Per IP: 10x merchant default
- Redis-backed (Bucket4j)
- 429 response with `Retry-After`

### 13.6 Compliance Posture

- GDPR-compatible soft delete
- Data retention policies per table
- Audit trail for every sensitive action
- No PCI scope (tokenization only)

---

## 14. Observability Requirements

### 14.1 Metrics (Micrometer → Prometheus)

**Business metrics:**
- `porp.payments.created{merchant,currency}`
- `porp.payments.authorized{provider,currency}`
- `porp.payments.failed{provider,error_code}`
- `porp.payments.amount{currency}` (histogram)
- `porp.settlements.discrepancies{type}`
- `porp.webhooks.delivered{merchant,status}`

**Technical metrics:**
- `http.server.requests` (Spring default)
- `porp.routing.decision.latency` (histogram)
- `porp.provider.latency{provider}` (histogram)
- `porp.provider.circuit.state{provider}`
- `porp.kafka.consumer.lag{topic,group}`
- `porp.batch.duration{job}`
- `hikaricp.connections.*`

### 14.2 Tracing (OpenTelemetry)

- Every payment gets a trace
- Spans: API → Idempotency → Router → Adapter → Provider
- Kafka producer/consumer spans
- Batch job spans
- Correlation ID propagated in headers and logs

### 14.3 Logging

- Structured JSON
- Fields: `timestamp`, `level`, `logger`, `message`, `correlationId`, `merchantId`, `paymentId`, `traceId`, `spanId`
- No PII
- Loki for aggregation

### 14.4 Dashboards (Grafana)

1. **Payments Overview** — throughput, success rate, latency, error breakdown
2. **Provider Health** — success rate, latency, circuit state per provider
3. **Routing** — decision latency, provider distribution, fallback rate
4. **Settlement** — pending, settled, discrepancies
5. **Reconciliation** — run history, discrepancies by type, duration
6. **Infrastructure** — DB, Redis, Kafka, JVM

### 14.5 Alerts

| Alert | Condition | Severity |
|---|---|---|
| Provider success rate drop | < 90% for 5 min | Critical |
| Routing latency spike | p95 > 500ms for 5 min | High |
| Circuit breaker open | Any provider for > 1 min | High |
| Reconciliation discrepancies | > 100 in a run | High |
| Kafka consumer lag | > 10k for 5 min | High |
| DLQ non-empty | Any messages | High |
| API error rate | > 1% for 5 min | High |
| Batch job failure | Any | High |

---

## 15. Testing Strategy

### 15.1 Test Pyramid

```
        ┌───────────────┐
        │   E2E / Load  │  (k6, few)
        ├───────────────┤
        │  Integration  │  (Testcontainers, many)
        ├───────────────┤
        │   Component   │  (@WebMvcTest, @DataJpaTest)
        ├───────────────┤
        │  Unit Tests   │  (JUnit 5, most)
        └───────────────┘
```

### 15.2 Per-Layer Strategy

| Layer | Tool | Coverage Target |
|---|---|---|
| Domain logic | JUnit 5 + AssertJ | 90% |
| State machine | `@SpringBootTest` | 100% transitions |
| Routing engine | JUnit 5 + parameterized | 90% |
| Provider adapters | WireMock + Spring Cloud Contract | 90% |
| Repositories | `@DataJpaTest` + Testcontainers | 80% |
| Controllers | `@WebMvcTest` / `@WebFluxTest` | 80% |
| Services | Mockito | 80% |
| Batch jobs | `@SpringBatchTest` + Testcontainers | 80% |
| Kafka flows | `@EmbeddedKafka` / Testcontainers | 80% |
| End-to-end | `@SpringBootTest` + Testcontainers | Key flows |
| Performance | k6 | NFR targets |
| Chaos | Toxiproxy | Provider failures |

### 15.3 Contract Testing

- Spring Cloud Contract per provider adapter
- Consumer-driven contracts for downstream services
- Contracts stored in repo, published to local Nexus (optional)

### 15.4 Test Data

- Testcontainers for Postgres, Redis, Kafka
- Flyway migrations run on test DB
- Fixtures via `@Sql` or builders
- No shared state between tests

### 15.5 CI Gates

- All tests pass
- Coverage thresholds met
- Spotless + Checkstyle pass
- SonarQube quality gate pass
- OWASP dependency check pass
- OpenAPI spec validation

---

## 16. Deployment & DevOps

### 16.1 Local Development

```bash
docker-compose up -d   # Postgres, Redis, Kafka, Vault, Prometheus, Grafana, Loki
./mvnw spring-boot:run
```

### 16.2 Build

- Maven multi-module (or single module with Spring Modulith)
- Multi-stage Dockerfile (build with Maven, run with distroless JRE)
- Jib optional for OCI image building

### 16.3 CI/CD (GitHub Actions)

**On PR:**
- Lint + format check
- Unit + integration tests (Testcontainers)
- Coverage report
- SonarQube scan
- OWASP dependency check
- Build Docker image (not push)

**On merge to `main`:**
- All of the above
- Push image to registry (GHCR)
- Deploy to staging
- Run smoke tests
- Optional: deploy to production

### 16.4 Environments

| Env | Purpose | Data |
|---|---|---|
| `local` | Developer | docker-compose |
| `test` | CI | Testcontainers |
| `staging` | Pre-prod | Sandbox providers, seeded data |
| `prod-demo` | Portfolio demo | Sandbox providers, synthetic traffic |

### 16.5 Deployment Target

- **Primary:** Fly.io or Railway (simple, cheap, good for demo)
- **Alternative:** AWS ECS Fargate + RDS + MSK + ElastiCache
- **Observability:** Grafana Cloud free tier or self-hosted

### 16.6 Runbook

- Health check: `/actuator/health`
- Readiness: `/actuator/health/readiness`
- Liveness: `/actuator/health/liveness`
- Metrics: `/actuator/prometheus`
- Rollback: redeploy previous image tag
- DB migrations: Flyway, forward-only, tested on staging first

---

## 17. Milestones & Timeline

Assumes **part-time effort (~15 hours/week)**. Full-time would compress to ~6 weeks.

| Wave | Weeks | Milestone | Exit Criteria |
|---|---|---|---|
| 1 | 1–3 | Walking skeleton | Payment flows through mock provider; CI green |
| 2 | 4–6 | Routing engine | Rules route to different providers; fallback works |
| 3 | 7–9 | Multi-provider + events | 3 providers integrated; Kafka events flowing |
| 4 | 10–12 | Settlement + reconciliation | Nightly batch reconciles 1M records |
| 5 | 13–15 | Enterprise hardening | OAuth2, multi-tenancy, Vault, observability live |
| 6 | 16–17 | Polish | README, ADRs, blog posts, demo, video |

### 17.1 Per-Wave Deliverables

Each wave produces:
- Working code (merged to `main`)
- Tests (unit + integration)
- Updated OpenAPI spec
- 1–2 ADRs
- 1 blog post (from Wave 2 onward)
- Demo video snippet (optional)

---

## 18. Success Metrics

### 18.1 Technical Metrics

| Metric | Target |
|---|---|
| Test coverage (domain) | ≥ 80% |
| Test coverage (overall) | ≥ 60% |
| Routing latency p95 | < 200ms |
| Payment API latency p95 | < 300ms |
| Reconciliation throughput | 1M records < 30 min |
| Zero payment loss in tests | 100% |
| CI pipeline duration | < 15 min |

### 18.2 Portfolio Metrics

| Metric | Target |
|---|---|
| ADRs written | ≥ 10 |
| Blog posts published | ≥ 3 |
| Architecture diagrams | ≥ 5 |
| Live demo uptime | ≥ 99% (demo window) |
| README completeness | All sections filled |
| Recruiter-friendly summary | 1 page |

### 18.3 Learning Outcomes

By completion, you will have hands-on experience with:

- Spring Boot 3.x, Java 21
- Spring WebFlux + WebMVC
- Spring Data JPA, JDBC, Redis, MongoDB, Elasticsearch
- Spring Batch (chunk, partition, restart)
- Spring Integration (adapters, transformers, filters)
- Spring State Machine (persist, actions, guards)
- Spring Kafka + Spring Cloud Stream
- Spring Security + Spring Authorization Server
- Spring Vault
- Spring Modulith
- Hibernate multi-tenancy
- Resilience4j, Spring Retry
- Quartz scheduling
- Micrometer, OpenTelemetry, Prometheus, Grafana
- Testcontainers, Spring Cloud Contract
- Docker, GitHub Actions, cloud deployment

---

## 19. Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Scope creep (too many providers) | High | High | Cap at 3 providers for v1.0; mock others |
| R2 | Spring Batch tuning complexity | Medium | High | Start with chunk-only; add partitioning only if needed |
| R3 | Multi-tenancy complexity | Medium | High | Defer to Wave 5; use schema-per-tenant (simpler than DB-per-tenant) |
| R4 | Kafka local dev friction | Medium | Medium | Use Testcontainers + `@EmbeddedKafka`; provide docker-compose |
| R5 | Time overrun | High | Medium | Waves are independent; can stop after Wave 4 and still have strong portfolio |
| R6 | Over-engineering | Medium | High | Enforce YAGNI; each Spring tech must have a domain reason |
| R7 | Real provider sandbox flakiness | Medium | Medium | Wrap providers in WireMock for tests; use sandbox only for demo |
| R8 | Security misconfiguration | Low | High | Follow Spring Security defaults; test with OWASP ZAP |
| R9 | Deployment cost | Medium | Low | Use free tiers; tear down after demo windows |
| R10 | Burnout | Medium | High | Weekly cadence; celebrate each wave |

---

## 20. Future Roadmap

Post-v1.0 ideas (not commitments):

- **v1.1** — Merchant dashboard (React or Thymeleaf)
- **v1.2** — Fraud rules engine (Drools) + ML scoring
- **v1.3** — Chargeback arbitration workflows
- **v1.4** — Payout service (merchant settlements → bank)
- **v1.5** — Multi-region deployment with active-passive
- **v1.6** — GraphQL admin API
- **v1.7** — Provider self-service onboarding
- **v1.8** — Event sourcing for full payment history replay
- **v1.9** — Migrate to microservices (only if justified)
- **v2.0** — Public sandbox for external developers

---

## 21. Appendices

### 21.1 Glossary

| Term | Definition |
|---|---|
| **Provider** | External payment company (Stripe, Adyen, etc.) |
| **Merchant** | Business using PORP to accept payments |
| **Authorization** | Provider holds funds on the customer's card |
| **Capture** | Provider actually takes the funds |
| **Settlement** | Provider deposits funds into PORP's account |
| **Payout** | PORP sends funds to the merchant |
| **Chargeback** | Customer disputes a charge via their bank |
| **Idempotency** | Same request produces same result, no duplicates |
| **Outbox** | Pattern for reliable event publishing |
| **Circuit Breaker** | Stops calls to a failing provider temporarily |
| **Fallback** | Trying the next provider after a retryable failure |
| **Multi-tenancy** | One system serving many isolated merchants |
| **ADR** | Architecture Decision Record |
| **DLQ** | Dead Letter Queue (failed messages) |

### 21.2 References

- Spring Boot 3.x documentation
- Spring Modulith reference
- Spring Batch reference
- Spring Integration reference
- Spring State Machine reference
- Spring Cloud Stream reference
- Enterprise Integration Patterns (Hohpe & Woolf)
- Building Microservices (Newman) — for patterns even in a monolith
- Release It! (Nygard) — circuit breakers, bulkheads
- Designing Data-Intensive Applications (Kleppmann)

### 21.3 ADR Index (Planned)

| ADR | Title |
|---|---|
| ADR-001 | Use modular monolith over microservices |
| ADR-002 | Use Spring Modulith for module boundaries |
| ADR-003 | Use PostgreSQL as primary datastore |
| ADR-004 | Use Redis for idempotency and caching |
| ADR-005 | Use Kafka for event streaming |
| ADR-006 | Use Outbox pattern for reliable publishing |
| ADR-007 | Use Spring State Machine for payment lifecycle |
| ADR-008 | Use Spring Batch for reconciliation |
| ADR-009 | Use schema-per-tenant multi-tenancy |
| ADR-010 | Use OAuth2 with Spring Authorization Server |
| ADR-011 | Use Vault for secrets management |
| ADR-012 | Use OpenTelemetry for tracing |

### 21.4 Repository Structure

```
payment-orchestration-platform/
├── .github/workflows/
├── docs/
│   ├── adr/
│   ├── diagrams/
│   ├── api/openapi.yaml
│   └── runbook.md
├── src/main/java/com/porp/
│   ├── merchant/
│   ├── payment/
│   ├── routing/
│   ├── provider/
│   ├── settlement/
│   ├── reconciliation/
│   ├── event/
│   ├── webhook/
│   ├── idempotency/
│   ├── security/
│   ├── tenancy/
│   ├── audit/
│   ├── admin/
│   └── shared/
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-staging.yml
│   ├── db/migration/
│   └── contracts/
├── src/test/java/com/porp/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
└── PRD.md
```

### 21.5 Definition of Done (per feature)

A feature is done when:

- [ ] Code implemented and reviewed
- [ ] Unit tests written (coverage ≥ 80%)
- [ ] Integration tests written (where applicable)
- [ ] OpenAPI spec updated
- [ ] Metrics and logs added
- [ ] ADR written (if architectural)
- [ ] README updated (if user-facing)
- [ ] CI green
- [ ] Merged to `main`

---

**End of PRD**

*This document is a living artifact. It will evolve as the project does. Every significant change is captured via an ADR.*