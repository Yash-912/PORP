# Payment Orchestration & Routing Platform — Implementation Plan

| Field | Value |
|---|---|
| **Companion Doc** | `docs/PRD.md` |
| **Version** | 1.0 |
| **Status** | Active |
| **Owner** | [Your Name] |
| **Last Updated** | 2026-01-15 |
| **Estimated Duration** | 17 weeks part-time (~15 hrs/week) |
| **Total Tickets** | 96 |

---

## Table of Contents

1. [How to Use This Document](#1-how-to-use-this-document)
2. [Prerequisites & Environment Setup](#2-prerequisites--environment-setup)
3. [Engineering Conventions](#3-engineering-conventions)
4. [Wave 0 — Bootstrap (Week 0)](#4-wave-0--bootstrap-week-0)
5. [Wave 1 — Walking Skeleton (Weeks 1–3)](#5-wave-1--walking-skeleton-weeks-13)
6. [Wave 2 — Routing Engine (Weeks 4–6)](#6-wave-2--routing-engine-weeks-46)
7. [Wave 3 — Multi-Provider + Events (Weeks 7–9)](#7-wave-3--multi-provider--events-weeks-79)
8. [Wave 4 — Settlement & Reconciliation (Weeks 10–12)](#8-wave-4--settlement--reconciliation-weeks-1012)
9. [Wave 5 — Enterprise Hardening (Weeks 13–15)](#9-wave-5--enterprise-hardening-weeks-1315)
10. [Wave 6 — Polish & Portfolio (Weeks 16–17)](#10-wave-6--polish--portfolio-weeks-1617)
11. [Cross-Cutting Concerns](#11-cross-cutting-concerns)
12. [Definition of Done](#12-definition-of-done)
13. [Weekly Cadence & Tracking](#13-weekly-cadence--tracking)
14. [Learning Resources Per Wave](#14-learning-resources-per-wave)
15. [Risk Register & Checkpoints](#15-risk-register--checkpoints)

---

## 1. How to Use This Document

This plan converts the PRD into **actionable tickets**. Every ticket has:

- **ID** — `W<wave>-T<n>` (e.g., `W1-T3`)
- **Title** — short description
- **Estimate** — hours (part-time pace)
- **Depends on** — prerequisite tickets
- **Deliverable** — concrete artifact
- **Acceptance criteria** — how you know it's done
- **Learning focus** — what Spring concept it teaches

**Workflow per ticket:**

1. Read the ticket fully.
2. Write the failing test first (if applicable).
3. Implement.
4. Verify acceptance criteria.
5. Commit with a conventional message.
6. Update the wave's tracking issue.
7. Move on.

**Rules:**

- Never start a ticket whose dependencies aren't done.
- Never skip the acceptance criteria.
- If a ticket takes >1.5x its estimate, stop and split it.
- Every wave ends with a demo video (5 min) and a retrospective note.

---

## 2. Prerequisites & Environment Setup

Complete **before Wave 0**. This is non-negotiable.

### 2.1 Knowledge Prerequisites

| Skill | Minimum Level |
|---|---|
| Java 21 | Records, sealed classes, streams, generics, exceptions |
| Maven | `pom.xml`, dependencies, plugins, lifecycle |
| Git | Branch, commit, merge, rebase, PR workflow |
| JUnit 5 | `@Test`, assertions, parameterized tests |
| REST | HTTP methods, status codes, headers, JSON |
| SQL | SELECT, JOIN, indexes, transactions |
| Docker | `docker run`, `docker-compose`, volumes, networks |

If any are weak, spend 1–2 weeks on them **before** starting.

### 2.2 Local Tooling

| Tool | Version | Purpose |
|---|---|---|
| JDK | 21 (Temurin) | Language runtime |
| Maven | 3.9+ | Build |
| IntelliJ IDEA | Community or Ultimate | IDE |
| Docker Desktop | Latest | Containers |
| Postman or Bruno | Latest | API testing |
| DBeaver or pgAdmin | Latest | DB inspection |
| k6 | Latest | Load testing (Wave 5) |
| Git | 2.40+ | Version control |

### 2.3 Accounts & Services

- **GitHub** — repo + Actions
- **Docker Hub** or **GHCR** — image registry (Wave 5)
- **Fly.io** or **Railway** — deployment target (Wave 6)
- **Stripe sandbox** — Wave 3
- **Adyen sandbox** — Wave 3
- **Grafana Cloud** (free tier) — optional, Wave 5

### 2.4 Baseline Verification

Run these commands. All must succeed before Wave 0.

```bash
java -version           # openjdk 21
mvn -version            # Apache Maven 3.9+
docker --version        # Docker version 24+
docker compose version  # Docker Compose version v2+
git --version           # git version 2.40+
```

---

## 3. Engineering Conventions

Lock these in before writing code. Consistency is what separates portfolios from toys.

### 3.1 Repository Structure

```
payment-orchestration-platform/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   └── release.yml
│   └── pull_request_template.md
├── docs/
│   ├── PRD.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── adr/
│   ├── diagrams/
│   ├── api/openapi.yaml
│   └── runbook.md
├── src/
│   ├── main/
│   │   ├── java/com/porp/
│   │   │   ├── PorpApplication.java
│   │   │   ├── merchant/
│   │   │   ├── payment/
│   │   │   ├── routing/
│   │   │   ├── provider/
│   │   │   ├── settlement/
│   │   │   ├── reconciliation/
│   │   │   ├── event/
│   │   │   ├── webhook/
│   │   │   ├── idempotency/
│   │   │   ├── security/
│   │   │   ├── tenancy/
│   │   │   ├── audit/
│   │   │   ├── admin/
│   │   │   └── shared/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-test.yml
│   │       ├── db/migration/
│   │       └── contracts/
│   └── test/
│       └── java/com/porp/
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── scripts/
│   ├── setup.sh
│   └── seed.sh
├── pom.xml
├── README.md
└── CHANGELOG.md
```

### 3.2 Package Structure (per module)

Each Spring Modulith module follows this internal structure:

```
com.porp.payment/
├── api/              (public interfaces used by other modules)
├── internal/         (implementation, not visible outside)
│   ├── web/          (controllers, DTOs)
│   ├── service/      (business logic)
│   ├── domain/       (entities, value objects, events)
│   ├── repository/   (Spring Data repositories)
│   └── config/       (module-local configuration)
├── PaymentModule.java (public façade)
└── package-info.java
```

**Rule:** Other modules may only reference `api/`, `PaymentModule.java`, and `package-info.java`. Spring Modulith enforces this.

### 3.3 Branching Model

- `main` — always green, deployable
- `feature/W<wave>-T<n>-short-description` — one branch per ticket
- `fix/...` — bug fixes
- `docs/...` — documentation only

Merge via **squash and merge** to keep history clean.

### 3.4 Commit Convention

Use **Conventional Commits**:

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`, `ci`, `build`, `style`

Examples:

```
feat(payment): add POST /payments endpoint with idempotency
test(routing): cover currency filter for USD/EUR
docs(adr): add ADR-005 (use Kafka for event streaming)
chore(ci): add SonarQube scan to PR workflow
```

### 3.5 Coding Standards

- **Java style:** Google Java Format via Spotless
- **Imports:** no wildcards, sorted
- **Naming:** `PascalCase` classes, `camelCase` methods, `UPPER_SNAKE` constants
- **Logging:** SLF4J, structured, no PII
- **Exceptions:** domain-specific, mapped to HTTP via `@ControllerAdvice`
- **DTOs:** records where possible
- **Entities:** never exposed via API
- **Money:** always `BigDecimal`, never `double`
- **Time:** always `Instant` or `OffsetDateTime`, UTC stored
- **Nulls:** use `Optional`, never return `null` from public methods

### 3.6 Testing Standards

- **Test naming:** `methodName_condition_expectedResult`
- **Structure:** given-when-then
- **Fixtures:** builder pattern, no test data classes
- **Isolation:** no shared state between tests
- **Coverage:** domain ≥80%, overall ≥60%
- **Integration:** Testcontainers for all external deps

### 3.7 Configuration Standards

- **Profiles:** `local`, `test`, `staging`, `prod-demo`
- **Secrets:** never in files, always via Vault or env vars
- **Feature flags:** `porp.features.<name>` in application.yml
- **12-factor:** everything configurable via environment

### 3.8 Documentation Standards

- **ADRs:** one per architectural decision, numbered
- **OpenAPI:** generated from code, checked in
- **README:** always current, runnable in <5 min
- **Code comments:** explain *why*, not *what*

---

## 4. Wave 0 — Bootstrap (Week 0)

**Goal:** A repo that builds, tests, and deploys a "hello world" API.

**Exit criteria:** `docker compose up && ./mvnw spring-boot:run` returns a healthy response.

### Tickets

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W0-T1 | Create GitHub repo and clone | 1h | — | Repo at `github.com/<you>/payment-orchestration-platform` |
| W0-T2 | Generate Spring Boot project via Initializr | 2h | W0-T1 | Working `pom.xml` with all dependencies |
| W0-T3 | Set up package structure and `PorpApplication` | 2h | W0-T2 | `com.porp` root package with modules as packages |
| W0-T4 | Configure application.yml for local profile | 2h | W0-T2 | `application-local.yml` with Postgres, Redis, Kafka placeholders |
| W0-T5 | Write `docker-compose.yml` for infra | 3h | W0-T4 | Postgres + Redis + Kafka running |
| W0-T6 | Add Flyway baseline migration | 2h | W0-T5 | `V1__baseline.sql` creates `merchants` table |
| W0-T7 | Implement `/actuator/health` and `/api/v1/ping` | 2h | W0-T3 | Both endpoints return 200 |
| W0-T8 | Add OpenAPI via springdoc | 2h | W0-T7 | Swagger UI at `/swagger-ui.html` |
| W0-T9 | Add GitHub Actions CI workflow | 3h | W0-T2 | `.github/workflows/ci.yml` runs build + test |
| W0-T10 | Write README skeleton | 2h | W0-T1 | README with run instructions |
| W0-T11 | Add Spotless + Checkstyle | 2h | W0-T2 | `./mvnw spotless:apply` works |
| W0-T12 | Commit and push, verify CI green | 1h | W0-T9 | Green check on GitHub |

**Total:** ~24 hours (Week 0)

### Key Dependencies (`pom.xml`)

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.x</version>
</parent>

<properties>
  <java.version>21</java.version>
</properties>

<dependencies>
  <!-- Core -->
  <dependency>spring-boot-starter-web</dependency>
  <dependency>spring-boot-starter-webflux</dependency>
  <dependency>spring-boot-starter-validation</dependency>
  <dependency>spring-boot-starter-actuator</dependency>

  <!-- Data -->
  <dependency>spring-boot-starter-data-jpa</dependency>
  <dependency>spring-boot-starter-data-jdbc</dependency>
  <dependency>spring-boot-starter-data-redis</dependency>
  <dependency>postgresql</dependency>
  <dependency>flyway-core</dependency>
  <dependency>flyway-database-postgresql</dependency>

  <!-- Messaging -->
  <dependency>spring-kafka</dependency>

  <!-- Security (added Wave 5) -->
  <!-- <dependency>spring-boot-starter-security</dependency> -->
  <!-- <dependency>spring-boot-starter-oauth2-resource-server</dependency> -->

  <!-- Integration & batch (added later waves) -->
  <!-- <dependency>spring-boot-starter-integration</dependency> -->
  <!-- <dependency>spring-boot-starter-batch</dependency> -->
  <!-- <dependency>spring-statemachine-starter</dependency> -->

  <!-- Observability -->
  <dependency>micrometer-registry-prometheus</dependency>
  <dependency>micrometer-tracing-bridge-otel</dependency>

  <!-- Docs -->
  <dependency>springdoc-openapi-starter-webmvc-ui</dependency>

  <!-- Modulith (Wave 2+) -->
  <dependency>spring-modulith-starter-core</dependency>

  <!-- Testing -->
  <dependency>spring-boot-starter-test</dependency>
  <dependency>spring-boot-testcontainers</dependency>
  <dependency>testcontainers</dependency>
  <dependency>testcontainers-postgresql</dependency>
  <dependency>testcontainers-kafka</dependency>
  <dependency>embedded-redis</dependency>
  <dependency>wiremock</dependency>
  <dependency>spring-cloud-contract-wiremock</dependency>
</dependencies>
```

### `docker-compose.yml` (baseline)

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: porp
      POSTGRES_USER: porp
      POSTGRES_PASSWORD: porp
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U porp"]
      interval: 5s

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  kafka:
    image: bitnami/kafka:3.7
    ports: ["9092:9092"]
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"

volumes:
  postgres-data:
```

---

## 5. Wave 1 — Walking Skeleton (Weeks 1–3)

**Goal:** A payment flows end-to-end through a mock provider, persisted and idempotent.

**Exit criteria:** `POST /api/v1/payments` with an idempotency key creates a payment, routes to mock provider, transitions through states, and duplicate requests return the same response.

**Milestone tag:** `v0.1.0-walking-skeleton`

### Week 1 — Domain & Persistence

| ID | Title | Est. | Depends on | Deliverable | Acceptance Criteria |
|---|---|---|---|---|---|
| W1-T1 | Design payment state enum & transitions | 2h | W0 | `PaymentState` enum, transition table | Unit test verifies valid/invalid transitions |
| W1-T2 | Create `Payment` JPA entity | 4h | W1-T1 | Entity + `@Version` | Persists and loads |
| W1-T3 | Create `PaymentEvent` entity (append-only) | 2h | W1-T2 | Entity, no setters | Insert-only verified in test |
| W1-T4 | Create `PaymentAttempt` entity | 2h | W1-T2 | Entity | Persists |
| W1-T5 | Create `Merchant` entity | 2h | W0 | Entity + Flyway migration | Seeded test merchant |
| W1-T6 | Create `Money` value object | 2h | — | `record Money(BigDecimal amount, Currency currency)` | Unit tests for add/subtract/zero |
| W1-T7 | Write Flyway migrations V2–V5 | 3h | W1-T2..T5 | Migration files | `flyway:migrate` runs clean |
| W1-T8 | Implement `PaymentRepository` | 2h | W1-T2 | Spring Data interface | `@DataJpaTest` green |
| W1-T9 | Implement `MerchantRepository` | 1h | W1-T5 | Interface | `@DataJpaTest` green |
| W1-T10 | Add Testcontainers base test class | 3h | W1-T8 | `AbstractIntegrationTest` | One test green |

**Learning focus:** JPA entities, `@Version`, Flyway, Testcontainers, value objects.

### Week 2 — State Machine & Idempotency

| ID | Title | Est. | Depends on | Deliverable | Acceptance Criteria |
|---|---|---|---|---|---|
| W1-T11 | Add Spring State Machine dependency | 1h | W1-T10 | Dependency in pom | Builds |
| W1-T12 | Configure `PaymentStateMachine` | 6h | W1-T11 | Config class with states/transitions/actions | All transitions unit tested |
| W1-T13 | Persist state transitions to `payment_events` | 4h | W1-T12 | Action listener | Every transition creates event row |
| W1-T14 | Implement `IdempotencyService` (Redis) | 4h | W0 | Service using `SETNX` | Duplicate returns cached response |
| W1-T15 | Add DB idempotency fallback | 3h | W1-T14 | `idempotency_keys` table + repo | Redis miss falls back to DB |
| W1-T16 | Implement `PaymentService.createPayment` | 6h | W1-T12, T14 | Service method | Integration test: creates payment, runs state machine |
| W1-T17 | Add global exception handler | 3h | W1-T16 | `@ControllerAdvice` | RFC 7807 responses |
| W1-T18 | Add domain exceptions | 2h | W1-T17 | `PaymentException`, `IdempotencyConflictException` | Mapped to correct statuses |

**Learning focus:** Spring State Machine, idempotency patterns, transactional boundaries, error mapping.

### Week 3 — API, Mock Provider, Tests

| ID | Title | Est. | Depends on | Deliverable | Acceptance Criteria |
|---|---|---|---|---|---|
| W1-T19 | Create `PaymentController` | 4h | W1-T16 | REST endpoints | `POST /payments` returns 201 |
| W1-T20 | Create request/response DTOs | 2h | W1-T19 | Records | Validation annotations |
| W1-T21 | Define `ProviderAdapter` interface | 3h | — | Interface with authorize/capture/refund | Documented |
| W1-T22 | Implement `MockProviderAdapter` | 5h | W1-T21 | Mock adapter with configurable outcomes | Unit tested |
| W1-T23 | Wire provider into state machine | 4h | W1-T22 | Action calls adapter | Integration test covers happy path |
| W1-T24 | Implement `PaymentAttempt` recording | 3h | W1-T23 | Attempt persisted per call | Verified in test |
| W1-T25 | Add `GET /payments/{id}` | 2h | W1-T19 | Endpoint | Returns payment + state |
| W1-T26 | Add `GET /payments/{id}/events` | 2h | W1-T19 | Endpoint | Returns event history |
| W1-T27 | Add `GET /payments/{id}/attempts` | 2h | W1-T19 | Endpoint | Returns attempts |
| W1-T28 | Write end-to-end integration test | 6h | All above | Testcontainers test | Full flow green |
| W1-T29 | Record demo video | 2h | W1-T28 | Loom or OBS | 5-minute walkthrough |

**Learning focus:** REST design, DTOs, adapter pattern, integration testing.

### Wave 1 Deliverables

- Working payment creation with idempotency
- State machine with 8+ states
- Mock provider adapter
- 4 REST endpoints
- 30+ tests
- Demo video

### Wave 1 Demo Script

1. `docker compose up`
2. `./mvnw spring-boot:run`
3. `curl -X POST .../payments` with idempotency key
4. Show response with `AUTHORIZED` state
5. Repeat request → same response (idempotency)
6. Show DB rows: payment, events, attempts
7. Show test suite green

---

## 6. Wave 2 — Routing Engine (Weeks 4–6)

**Goal:** Payments route across multiple mock providers using configurable rules with failover.

**Exit criteria:** Routing decision logged with rationale; circuit breaker opens on failure; fallback works.

**Milestone tag:** `v0.2.0-routing`

### Week 4 — Merchant Config & Provider Registry

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W2-T1 | Create `merchant_providers` table + entity | 3h | Wave 1 | Merchant-provider link |
| W2-T2 | Create `routing_rules` table + entity | 3h | W2-T1 | Rule storage |
| W2-T3 | Implement `MerchantConfigService` | 4h | W2-T1 | CRUD for merchant config |
| W2-T4 | Implement `ProviderRegistry` | 4h | W2-T1 | List of enabled providers per merchant |
| W2-T5 | Add second mock provider | 3h | W1-T22 | `MockProviderB` |
| W2-T6 | Add third mock provider | 3h | W2-T5 | `MockProviderC` |
| W2-T7 | Cache merchant config in Redis | 3h | W2-T3 | `@Cacheable` on config |
| W2-T8 | Add config cache invalidation | 2h | W2-T7 | Evict on update |
| W2-T9 | Add admin API for merchant config | 4h | W2-T3 | Admin endpoints |
| W2-T10 | Write tests for config service | 3h | W2-T3 | 80% coverage |

**Learning focus:** Spring Data JPA, caching, admin APIs.

### Week 5 — Routing Logic

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W2-T11 | Design `RoutingContext` | 2h | W2-T4 | Input to router |
| W2-T12 | Define `RoutingStrategy` interface | 2h | W2-T11 | `List<ProviderId> route(RoutingContext)` |
| W2-T13 | Implement `CostBasedStrategy` | 4h | W2-T12 | Sorts by cost |
| W2-T14 | Implement `SuccessRateStrategy` | 4h | W2-T12 | Sorts by success rate |
| W2-T15 | Implement `WeightedStrategy` | 5h | W2-T12 | Weighted scoring |
| W2-T16 | Implement provider filtering (currency, method) | 4h | W2-T12 | Filter chain |
| W2-T17 | Implement `RoutingEngine` façade | 4h | W2-T13..T16 | Returns ranked list + rationale |
| W2-T18 | Record routing decision | 3h | W2-T17 | Persist decision + reasons |
| W2-T19 | Add routing decision endpoint | 2h | W2-T18 | `GET /payments/{id}/routing` |
| W2-T20 | Write routing unit tests | 6h | W2-T17 | 90% coverage, parameterized |

**Learning focus:** Strategy pattern, composition, domain modeling.

### Week 6 — Resilience & Fallback

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W2-T21 | Add Resilience4j dependency | 1h | — | pom updated |
| W2-T22 | Configure circuit breaker per provider | 4h | W2-T21 | Config in yaml |
| W2-T23 | Wrap adapter calls in circuit breaker | 4h | W2-T22 | Decorated calls |
| W2-T24 | Add retry with exponential backoff | 4h | W2-T21 | Spring Retry |
| W2-T25 | Implement provider health tracking | 5h | W2-T23 | Rolling metrics in Redis |
| W2-T26 | Implement fallback chain | 5h | W2-T17 | Try next provider on retryable error |
| W2-T27 | Add error taxonomy mapping | 4h | W1-T22 | Provider error → internal code |
| W2-T28 | Write circuit breaker integration tests | 4h | W2-T22 | Toxiproxy or mock failures |
| W2-T29 | Write fallback integration tests | 4h | W2-T26 | First fails, second succeeds |
| W2-T30 | Add circuit state endpoint | 2h | W2-T23 | `GET /admin/providers/health` |
| W2-T31 | Record demo video | 2h | All above | Failover walkthrough |

**Learning focus:** Resilience4j, circuit breakers, retry patterns, fallback.

### Wave 2 Deliverables

- 3 mock providers
- 3 routing strategies
- Circuit breakers per provider
- Fallback routing
- Routing audit trail
- 40+ new tests
- Demo video showing failover

---

## 7. Wave 3 — Multi-Provider + Events (Weeks 7–9)

**Goal:** Real sandbox providers integrated; Kafka events flowing via Outbox.

**Exit criteria:** A payment routed to Stripe sandbox emits events; notification service consumes and logs; webhook delivered to merchant (mock).

**Milestone tag:** `v0.3.0-events`

### Week 7 — Kafka + Outbox

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W3-T1 | Configure Kafka producer | 3h | Wave 2 | Producer factory |
| W3-T2 | Create `outbox` table + entity | 3h | Wave 2 | Outbox storage |
| W3-T3 | Write event to outbox in same TX | 4h | W3-T2 | Transactional write |
| W3-T4 | Implement outbox publisher | 5h | W3-T3 | Scheduled publisher |
| W3-T5 | Mark outbox rows as published | 2h | W3-T4 | Update after publish |
| W3-T6 | Define event envelope | 2h | — | Common record |
| W3-T7 | Define event types | 3h | W3-T6 | Sealed interface + records |
| W3-T8 | Publish `PaymentCreated` | 2h | W3-T7 | Via outbox |
| W3-T9 | Publish `PaymentAuthorized` | 2h | W3-T7 | Via outbox |
| W3-T10 | Publish `PaymentAuthorizationFailed` | 2h | W3-T7 | Via outbox |
| W3-T11 | Write outbox integration test | 4h | W3-T4 | Testcontainers Kafka |
| W3-T12 | Add `@EmbeddedKafka` test config | 2h | W3-T1 | Reusable config |

**Learning focus:** Kafka, Outbox pattern, transactional messaging.

### Week 8 — Consumers & Webhooks

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W3-T13 | Create notification module | 3h | W3-T8..T10 | New package |
| W3-T14 | Consume `PaymentAuthorized` | 3h | W3-T13 | `@KafkaListener` |
| W3-T15 | Idempotent consumer (dedup by eventId) | 4h | W3-T14 | Redis check |
| W3-T16 | Implement webhook dispatcher | 5h | W3-T13 | HTTP POST with HMAC |
| W3-T17 | Implement webhook retry | 4h | W3-T16 | Exponential backoff |
| W3-T18 | Store webhook delivery log | 3h | W3-T16 | New table + entity |
| W3-T19 | Add DLQ for failed events | 4h | W3-T14 | Kafka DLQ topic |
| W3-T20 | Add merchant webhook config | 3h | W3-T16 | Endpoint URL + secret |
| W3-T21 | Write webhook integration tests | 4h | W3-T16 | WireMock |
| W3-T22 | Write DLQ tests | 3h | W3-T19 | Verify routing to DLQ |

**Learning focus:** Kafka consumers, idempotency, webhooks, DLQ.

### Week 9 — Real Provider Adapters

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W3-T23 | Sign up for Stripe sandbox | 1h | — | API keys in Vault (dev) |
| W3-T24 | Implement `StripeAdapter` | 8h | W1-T21 | Real Stripe calls |
| W3-T25 | Map Stripe errors to taxonomy | 4h | W3-T24 | Error mapper |
| W3-T26 | Sign up for Adyen sandbox | 1h | — | API keys |
| W3-T27 | Implement `AdyenAdapter` | 8h | W1-T21 | Real Adyen calls |
| W3-T28 | Map Adyen errors | 4h | W3-T27 | Error mapper |
| W3-T29 | Implement Stripe webhook receiver | 4h | W3-T24 | Signature verification |
| W3-T30 | Contract test for Stripe | 4h | W3-T24 | Spring Cloud Contract |
| W3-T31 | Contract test for Adyen | 4h | W3-T27 | Spring Cloud Contract |
| W3-T32 | Record demo video | 2h | All above | Real provider flow |

**Learning focus:** Real integrations, signature verification, contract testing.

### Wave 3 Deliverables

- Kafka + Outbox working
- 14 event types defined
- Notification module with webhooks
- 2 real sandbox provider adapters
- 50+ new tests
- Demo video

---

## 8. Wave 4 — Settlement & Reconciliation (Weeks 10–12)

**Goal:** Nightly batch reconciles 1M settlement records in under 30 minutes.

**Exit criteria:** Batch job processes settlement files, matches to payments, detects discrepancies, reports results.

**Milestone tag:** `v0.4.0-settlement`

### Week 10 — Settlement Service

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W4-T1 | Create `settlements` table + entity | 3h | Wave 3 | Schema |
| W4-T2 | Create `settlement_files` table | 2h | W4-T1 | Track ingested files |
| W4-T3 | Define settlement file formats (CSV, JSON) | 3h | W4-T1 | Format spec docs |
| W4-T4 | Implement file parser (CSV) | 5h | W4-T3 | Parser service |
| W4-T5 | Implement file parser (JSON) | 3h | W4-T3 | Parser service |
| W4-T6 | Implement settlement line matcher | 6h | W4-T1 | Match to payment |
| W4-T7 | Detect discrepancies | 5h | W4-T6 | 4 discrepancy types |
| W4-T8 | Persist settlement results | 3h | W4-T6 | Service |
| W4-T9 | Emit `PaymentSettled` event | 3h | W4-T8 | Via outbox |
| W4-T10 | Add settlement API | 3h | W4-T8 | `GET /settlements/{id}` |
| W4-T11 | Write settlement tests | 5h | W4-T8 | Integration tests |

**Learning focus:** File parsing, matching algorithms, discrepancy detection.

### Week 11 — Batch Reconciliation

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W4-T12 | Add Spring Batch dependency | 1h | — | pom updated |
| W4-T13 | Configure batch job repository | 3h | W4-T12 | Metadata tables |
| W4-T14 | Define `reconciliationJob` | 5h | W4-T13 | Job definition |
| W4-T15 | Implement `settlementFileReader` | 4h | W4-T14 | ItemReader |
| W4-T16 | Implement `settlementLineProcessor` | 4h | W4-T14 | ItemProcessor |
| W4-T17 | Implement `settlementWriter` | 3h | W4-T14 | ItemWriter |
| W4-T18 | Configure chunk size + skip policy | 3h | W4-T14 | Chunk 500 |
| W4-T19 | Add job listener for metrics | 3h | W4-T14 | Micrometer |
| W4-T20 | Add restartability | 4h | W4-T14 | Job parameters |
| W4-T21 | Add partitioning (optional) | 6h | W4-T20 | Partition by provider |
| W4-T22 | Write batch tests | 6h | W4-T14 | `@SpringBatchTest` |
| W4-T23 | Create sample settlement files (1M rows) | 3h | W4-T15 | Generator script |
| W4-T24 | Benchmark job on 1M rows | 4h | W4-T23 | Report duration |

**Learning focus:** Spring Batch (chunk, partition, restart, skip).

### Week 12 — Discrepancy Reporting & Scheduling

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W4-T25 | Create `reconciliation_runs` table | 2h | W4-T14 | Track runs |
| W4-T26 | Persist per-run summary | 3h | W4-T25 | Entity + service |
| W4-T27 | Persist discrepancies | 3h | W4-T25 | Entity + service |
| W4-T28 | Add admin endpoint for runs | 3h | W4-T25 | `GET /admin/reconciliation/runs` |
| W4-T29 | Add admin endpoint for discrepancies | 3h | W4-T27 | Filter by type |
| W4-T30 | Schedule nightly job with Quartz | 4h | W4-T14 | Cron 2am UTC |
| W4-T31 | Auto-resolve trivial discrepancies | 5h | W4-T27 | Matching rules |
| W4-T32 | Emit reconciliation summary event | 3h | W4-T26 | Via outbox |
| W4-T33 | Write report tests | 4h | W4-T31 | Coverage |
| W4-T34 | Record demo video | 2h | All above | Batch run walkthrough |

**Learning focus:** Quartz scheduling, reporting, observability.

### Wave 4 Deliverables

- Settlement service with 2 file parsers
- Spring Batch reconciliation job
- 1M row benchmark report
- Nightly scheduled job
- 60+ new tests
- Demo video

---

## 9. Wave 5 — Enterprise Hardening (Weeks 13–15)

**Goal:** Production-like security, multi-tenancy, observability, and performance.

**Exit criteria:** OAuth2 working; tenants isolated; Vault-integrated; Prometheus + Grafana dashboards; load test at 1000 RPS p95 <300ms.

**Milestone tag:** `v0.5.0-enterprise`

### Week 13 — Security

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W5-T1 | Add Spring Security + OAuth2 | 2h | — | pom updated |
| W5-T2 | Configure Spring Authorization Server | 6h | W5-T1 | Auth server with client credentials |
| W5-T3 | Define scopes and roles | 3h | W5-T2 | `payments:read`, `payments:write`, etc. |
| W5-T4 | Secure merchant API | 4h | W5-T2 | JWT validation |
| W5-T5 | Secure admin API | 3h | W5-T2 | Role-based |
| W5-T6 | Add method security | 3h | W5-T4 | `@PreAuthorize` |
| W5-T7 | Add tenant extraction from token | 4h | W5-T4 | `TenantContext` |
| W5-T8 | Add rate limiting (Bucket4j) | 5h | W5-T4 | Per merchant + IP |
| W5-T9 | Add Vault integration | 6h | — | `spring-cloud-vault` |
| W5-T10 | Move provider secrets to Vault | 3h | W5-T9 | No secrets in files |
| W5-T11 | Add audit log AOP | 5h | W5-T6 | `@Audited` aspect |
| W5-T12 | Write security tests | 5h | W5-T6 | OAuth2 test client |

**Learning focus:** Spring Security, OAuth2, Vault, AOP.

### Week 14 — Multi-Tenancy & Observability

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W5-T13 | Configure Hibernate multi-tenancy | 6h | W5-T7 | Schema-per-tenant |
| W5-T14 | Implement `TenantResolver` | 4h | W5-T13 | From context |
| W5-T15 | Migrate tenant tables to schemas | 4h | W5-T13 | Flyway per schema |
| W5-T16 | Make batch jobs tenant-aware | 5h | W5-T13 | Iterate tenants |
| W5-T17 | Write multi-tenancy tests | 6h | W5-T13 | Isolation verified |
| W5-T18 | Add correlation ID filter | 3h | — | MDC propagation |
| W5-T19 | Add Micrometer business metrics | 4h | — | Custom counters |
| W5-T20 | Add OpenTelemetry tracing | 5h | — | OTLP exporter |
| W5-T21 | Configure Prometheus endpoint | 2h | W5-T19 | `/actuator/prometheus` |
| W5-T22 | Add Grafana dashboards | 6h | W5-T21 | 6 dashboards |
| W5-T23 | Add structured JSON logging | 3h | W5-T18 | logback config |
| W5-T24 | Add alert rules | 4h | W5-T22 | 8 alerts |

**Learning focus:** Multi-tenancy, observability, tracing, dashboards.

### Week 15 — Performance & Load

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W5-T25 | Write k6 script for payment API | 4h | — | `load-tests/payments.js` |
| W5-T26 | Run baseline load test | 3h | W5-T25 | Report |
| W5-T27 | Tune connection pool | 3h | W5-T26 | HikariCP config |
| W5-T28 | Tune Kafka producer | 3h | W5-T26 | Batching config |
| W5-T29 | Tune batch job | 4h | W4-T21 | Partitioning if needed |
| W5-T30 | Run load test at 1000 RPS | 3h | W5-T27..T29 | p95 <300ms |
| W5-T31 | Add chaos test (Toxiproxy) | 4h | W5-T30 | Provider failure recovery |
| W5-T32 | Fix performance regressions | 6h | W5-T30 | Metrics green |
| W5-T33 | Write performance report | 3h | W5-T30 | Docs |
| W5-T34 | Record demo video | 2h | All above | Security + dashboards |

**Learning focus:** Performance tuning, load testing, chaos engineering.

### Wave 5 Deliverables

- OAuth2 with scopes and roles
- Multi-tenant isolation
- Vault-managed secrets
- Prometheus + Grafana + tracing
- k6 performance report
- 70+ new tests
- Demo video

---

## 10. Wave 6 — Polish & Portfolio (Weeks 16–17)

**Goal:** Everything a hiring manager needs to say yes.

**Exit criteria:** Live demo running; README compelling; 3+ blog posts; 12 ADRs; 5-min video.

**Milestone tag:** `v1.0.0`

### Week 16 — Documentation

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W6-T1 | Write ADR-001 (modular monolith) | 2h | — | `docs/adr/001-*.md` |
| W6-T2 | Write ADR-002 (Spring Modulith) | 2h | — | ADR file |
| W6-T3 | Write ADR-003 (Postgres) | 2h | — | ADR file |
| W6-T4 | Write ADR-004 (Redis idempotency) | 2h | — | ADR file |
| W6-T5 | Write ADR-005 (Kafka events) | 2h | — | ADR file |
| W6-T6 | Write ADR-006 (Outbox pattern) | 2h | — | ADR file |
| W6-T7 | Write ADR-007 (State machine) | 2h | — | ADR file |
| W6-T8 | Write ADR-008 (Spring Batch) | 2h | — | ADR file |
| W6-T9 | Write ADR-009 (Multi-tenancy) | 2h | — | ADR file |
| W6-T10 | Write ADR-010 (OAuth2) | 2h | — | ADR file |
| W6-T11 | Write ADR-011 (Vault) | 2h | — | ADR file |
| W6-T12 | Write ADR-012 (OpenTelemetry) | 2h | — | ADR file |
| W6-T13 | Generate architecture diagrams | 5h | — | Mermaid or draw.io |
| W6-T14 | Finalize OpenAPI spec | 3h | — | `openapi.yaml` |
| W6-T15 | Write runbook | 4h | — | `docs/runbook.md` |
| W6-T16 | Rewrite README | 6h | — | Complete README |
| W6-T17 | Create Postman collection | 3h | — | Exported JSON |

### Week 17 — Content & Demo

| ID | Title | Est. | Depends on | Deliverable |
|---|---|---|---|---|
| W6-T18 | Deploy to Fly.io | 6h | Wave 5 | Live demo URL |
| W6-T19 | Add uptime monitor | 2h | W6-T18 | UptimeRobot or similar |
| W6-T20 | Write blog post 1 (routing) | 5h | — | Published post |
| W6-T21 | Write blog post 2 (idempotency) | 5h | — | Published post |
| W6-T22 | Write blog post 3 (Spring Batch) | 5h | — | Published post |
| W6-T23 | Write blog post 4 (Outbox pattern) | 5h | — | Published post |
| W6-T24 | Record 5-min demo video | 4h | W6-T18 | Loom or YouTube |
| W6-T25 | Write recruiter-friendly 1-pager | 3h | All above | `PORTFOLIO.md` |
| W6-T26 | Add LICENSE | 1h | — | MIT or Apache 2.0 |
| W6-T27 | Add CONTRIBUTING.md | 2h | — | Guide |
| W6-T28 | Final push + tag v1.0.0 | 1h | All above | Git tag |
| W6-T29 | LinkedIn announcement post | 2h | W6-T24 | Published |

### Wave 6 Deliverables

- 12 ADRs
- 5 architecture diagrams
- 4 blog posts
- Live demo
- 5-min video
- Portfolio 1-pager
- `v1.0.0` tag

---

## 11. Cross-Cutting Concerns

These are **not** separate tickets. Every ticket must respect them.

### 11.1 Testing

- Unit tests for every service method
- Integration tests for every controller
- Testcontainers for all external deps
- No mocking of `JpaRepository`
- Coverage gate in CI

### 11.2 Error Handling

- Domain exceptions in `domain/`
- Mapped via `@ControllerAdvice`
- RFC 7807 responses
- Log at correct level (WARN for business, ERROR for system)
- No stack traces in API responses

### 11.3 Logging

- SLF4J only, no `System.out`
- Structured JSON (logback config)
- Correlation ID in every log
- No PII, no secrets
- Log levels: INFO for state changes, DEBUG for details, WARN for retries, ERROR for failures

### 11.4 Security

- Input validation on every endpoint
- No `@CrossOrigin("*")`
- No secrets in code
- Principle of least privilege
- Audit every write operation

### 11.5 Documentation

- Public method: Javadoc with `@param` and `@return`
- Complex logic: inline comment explaining *why*
- Public API: OpenAPI annotation
- Every module: `package-info.java` describing responsibility

---

## 12. Definition of Done

A ticket is done when **all** are true:

- [ ] Code compiles with no warnings
- [ ] Unit tests written and green
- [ ] Integration test written (where applicable)
- [ ] Coverage thresholds met
- [ ] Spotless applied
- [ ] Checkstyle passes
- [ ] SonarQube no blockers (Wave 2+)
- [ ] OpenAPI updated (if API changed)
- [ ] Metrics/logs added (if runtime path)
- [ ] ADR written (if architectural)
- [ ] README updated (if user-facing)
- [ ] Committed with conventional message
- [ ] Pushed, CI green
- [ ] Merged to `main`
- [ ] Ticket closed with link to commit

---

## 13. Weekly Cadence & Tracking

### 13.1 GitHub Project Board

Columns:

- **Backlog** — all tickets not yet started
- **This Week** — tickets planned for current week
- **In Progress** — actively being worked (max 1)
- **Review** — self-review before merge
- **Done** — merged and verified

### 13.2 Weekly Rhythm

| Day | Focus | Time |
|---|---|---|
| Monday | Plan week: move tickets to "This Week" | 30 min |
| Tue–Thu | Implement 1–2 tickets per day | 3–4 hrs/day |
| Friday | Tests, docs, merge, demo video snippet | 3 hrs |
| Saturday | Blog post or ADR (from Wave 2) | 2 hrs |
| Sunday | Rest | — |

### 13.3 End-of-Wave Checklist

- [ ] All tickets merged
- [ ] Demo video recorded
- [ ] Retrospective written (what worked, what didn't)
- [ ] Next wave planned
- [ ] Milestone tag pushed
- [ ] Blog post published (Wave 2+)

### 13.4 Weekly Retrospective Template

```markdown
## Week <n> Retrospective

**Completed:**
- <ticket IDs>

**Incomplete:**
- <ticket IDs + reason>

**Blockers hit:**
- <description + resolution>

**Key learnings:**
- <Spring concept learned>

**Next week's focus:**
- <tickets>
```

---

## 14. Learning Resources Per Wave

| Wave | Topic | Resource |
|---|---|---|
| 0 | Spring Boot basics | Spring Boot Reference, Baeldung intro |
| 1 | JPA, State Machine | Vlad Mihalcea blog, Spring State Machine docs |
| 2 | Resilience4j, caching | Resilience4j docs, Spring Cache docs |
| 3 | Kafka, Outbox | Confluent docs, microservices.io Outbox |
| 4 | Spring Batch | Spring Batch in Action, official docs |
| 5 | Security, Multi-tenancy | Spring Security in Action, Baeldung multi-tenancy |
| 6 | Writing, portfolio | "Technical Blogging" by Antonio Cangiano |

---

## 15. Risk Register & Checkpoints

### 15.1 Checkpoints (Stop and Reassess)

| After | Decision Point |
|---|---|
| Wave 1 | Is the walking skeleton solid? If not, fix before Wave 2. |
| Wave 3 | Are real provider integrations working? If not, mock them. |
| Wave 4 | Is batch performance acceptable? If not, tune before Wave 5. |
| Wave 5 | Are security and observability actually working? If not, cut features. |
| Wave 6 | Is the portfolio story compelling? If not, rewrite README. |

### 15.2 Cut Scope If Behind

Priority order (drop from bottom up):

1. Keep: Wave 1, 2, 3 core
2. Keep: Wave 4 batch job (simplified)
3. Drop: Wave 5 chaos tests
4. Drop: Wave 5 some dashboards
5. Drop: Wave 6 blog posts (keep 1)
6. Drop: Wave 3 third provider

### 15.3 Burnout Prevention

- Never work >20 hrs/week
- Take one full rest day per week
- Celebrate each wave (real reward)
- If behind by 2 weeks, cut scope, don't extend timeline
- Take a full week off between Wave 4 and 5

---

## Appendix A — Ticket Summary by Wave

| Wave | Tickets | Hours | Weeks |
|---|---|---|---|
| 0 | 12 | 24 | 0 |
| 1 | 29 | 90 | 3 |
| 2 | 31 | 105 | 3 |
| 3 | 32 | 120 | 3 |
| 4 | 34 | 125 | 3 |
| 5 | 34 | 130 | 3 |
| 6 | 29 | 95 | 2 |
| **Total** | **201** | **~689 hrs** | **17** |

*Adjust hours to your pace. If you have 10 hrs/week, expect ~69 weeks. If 20 hrs/week, ~35 weeks.*

---

## Appendix B — Quick Command Reference

```bash
# Start infra
docker compose up -d

# Run app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run all tests
./mvnw verify

# Run one test class
./mvnw test -Dtest=PaymentServiceTest

# Run integration tests only
./mvnw verify -Pintegration

# Format code
./mvnw spotless:apply

# Generate OpenAPI
./mvnw springdoc:generate

# Flyway migrate
./mvnw flyway:migrate

# Build image
docker build -t porp:local .

# Run batch job manually
curl -X POST localhost:8080/admin/reconciliation/runs

# Check health
curl localhost:8080/actuator/health
```

---

**End of Implementation Plan**

