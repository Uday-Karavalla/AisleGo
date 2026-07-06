# AisleGo — Testing Strategy

**Related docs:** `04-architecture.md`, `08-security-and-fraud-control.md` (invariants under test), `06-api-specification.md`

---

## 1. Testing Pyramid Overview

```
                    /\
                   /  \      E2E (Playwright)
                  /----\     — golden-path journeys, few, slow, high-confidence
                 /      \
                /--------\   Contract tests (REST API)
               /          \  — provider/consumer contracts per module boundary
              /------------\
             /              \  Integration tests (Testcontainers)
            /                \ — module + Postgres/Redis, many, medium speed
           /------------------\
          /                    \  Unit tests (JUnit 5 + Mockito, Vitest + RTL)
         /______________________\ — fast, most numerous, run on every save
```

The pyramid shape is deliberate: most confidence comes from fast, numerous unit tests; integration and contract tests confirm modules and layers actually cooperate; a small number of E2E tests confirm the real user-facing golden path works end-to-end. Concurrency/load tests sit alongside the pyramid as a separate, invariant-focused track (§5–6) because they test a different property (correctness/performance under contention) than functional correctness.

## 2. Unit Tests

**Backend — JUnit 5 + Mockito**

- Every module's service layer is unit-tested in isolation, with collaborating modules/repositories mocked at their public interface boundary (matching the module-boundary discipline in `04-architecture.md`).
- Priority coverage areas: order state-machine transition logic (only legal transitions permitted, e.g. `PICKING` cannot jump straight to `DELIVERED`), price/discount calculation, coupon eligibility rules, OTP generation/hashing/expiry logic, idempotency-key handling logic, inventory reservation/release calculation logic (unit-level; concurrency behavior is covered separately in §5).
- Target: fast enough to run on every local save and on every CI push without noticeable delay; no network, no database, no filesystem access in this tier.

**Frontend — Vitest + React Testing Library**

- Component-level tests for cart behavior (including the cross-store-conflict modal), checkout form validation, order-tracking status rendering, and form components used across supermarket/admin dashboards.
- Hooks and state-management logic (cart state, auth token refresh handling) unit-tested independent of rendered UI where practical.
- Accessibility assertions (labels, roles, focus order) included as part of component tests for primary customer flows, reflecting the accessibility requirements in `01-PRD.md`.

## 3. Integration Tests

**Testcontainers-backed module tests**

- Each module with a database dependency is tested against a real, ephemeral PostgreSQL instance spun up via Testcontainers — not H2 or a mocked repository — so that Postgres-specific behavior (constraints, triggers, `SELECT ... FOR UPDATE`, JSONB handling) is exercised faithfully. This is critical given how much of the single-supermarket-order rule and the inventory-reservation strategy is expressed as real database constraints/triggers (`05-database-er-diagram.md` §3, `08-security-and-fraud-control.md` §4).
- Redis-backed behavior (session/refresh-token revocation, rate-limiting counters, reservation-adjacent locks) is similarly tested against a real ephemeral Redis container rather than an in-memory fake.
- Representative integration test cases:
  - Adding a product from Supermarket B to a cart already containing Supermarket A's product returns the documented `409` and leaves the cart unchanged.
  - The `order_items` trigger rejects an insert whose product doesn't belong to the order's `supermarket_id`, even if application-layer checks are bypassed in the test harness.
  - A checkout that times out before payment confirmation results in the reservation being released and stock becoming available again.
  - An OTP that has exceeded its retry limit is rejected even with the correct code, and requires regeneration.

## 4. Contract Tests

- The REST API surface (`06-api-specification.md`) is covered by consumer-driven contract tests between the frontend clients (customer PWA, supermarket dashboard, delivery-partner PWA) and the backend, verifying that request/response shapes — including error shapes like `CrossStoreCartError` and `StockValidationError` — remain stable across changes.
- Contract tests run against the OpenAPI schema itself where practical (schema-validation of real request/response payloads in integration tests) so that API drift from the documented contract in `06-api-specification.md` §9 is caught automatically rather than relying on manual doc upkeep.
- The payment-gateway webhook contract is tested against the gateway's published schema/sandbox (mocked in Phase 0 against the internal mock gateway's documented event shape; revalidated against the real gateway's contract once integrated in Phase 1).

## 5. End-to-End Tests (Playwright)

E2E tests are kept deliberately few and focused on the golden path plus its most important branches, since they are the slowest and most brittle tier:

- **Golden path:** location detection → store discovery → open storefront → search/browse → add to cart → checkout (address + fulfillment + mock payment) → order placed → status progresses to `Delivered` (driven via test hooks/API for the store/delivery side) → customer rates the order.
- **Cross-store cart guard:** attempting to add a second store's product mid-journey surfaces the conflict modal and resolves correctly via "Clear cart and add."
- **Store-side accept/reject and substitution:** a store rejects an order (customer sees cancellation + refund), and separately, a store proposes a substitution that the customer must approve before the order proceeds.
- **Delivery OTP flow:** pickup OTP and delivery OTP verification, including one deliberately-wrong-OTP-then-correct-OTP retry path.
- E2E tests run against a fully containerized environment (the same Docker Compose stack described in `11-deployment-architecture.md`) in CI on a slower, less frequent cadence than unit/integration tests (e.g. on merge to main and pre-release, not on every commit).

## 6. Load Testing (k6) — Checkout & Inventory-Reservation Hot Path

- The checkout endpoint and its inventory-reservation logic are the platform's most contention-prone and business-critical hot path, and are load-tested explicitly with k6 rather than assumed safe from functional tests alone.
- Scenarios:
  - **Sustained load:** steady checkout request rate at projected peak-hour volume for a single busy branch, measuring p95/p99 latency and error rate.
  - **Spike/flash-sale simulation:** a burst of concurrent checkout requests targeting the same low-stock product at the same branch, verifying that overselling never occurs and that the optimistic-lock retry/fallback path (`08-security-and-fraud-control.md` §4) degrades gracefully (higher latency, correct `422` rejections) rather than failing incorrectly.
  - **Payment-gateway latency injection:** artificially slow/failing mock-gateway responses during load, verifying reservation timeout/release behavior holds up under load, not just in isolated integration tests.
- Load test results feed capacity planning for Phase 2 (when Kafka/OpenSearch are introduced) and inform which module is the first realistic extraction candidate under real load evidence (`04-architecture.md` §5).

## 7. Concurrency Tests for Core Invariants

Beyond generic load testing, two invariants are specifically targeted with dedicated concurrency test suites, because a functional bug here directly contradicts the product's core differentiator and trust model:

- **Single-supermarket-cart invariant under concurrency:** concurrent requests from the same customer session attempting to add items from two different supermarkets simultaneously (race on an initially-empty cart) must resolve deterministically to exactly one supermarket winning the cart scope, with the other request receiving a clean `409`, never a cart left in an inconsistent (dual-supermarket) state. Verified with repeated randomized concurrent-request test runs against the Testcontainers-backed integration environment, and backstopped by the DB trigger described in `05-database-er-diagram.md` §3, which makes an inconsistent persisted state structurally impossible even if the application-level race were somehow lost.
- **Stock-reservation invariant under concurrency:** many concurrent checkout attempts against a branch's limited stock of one product must never result in `reserved_quantity + fulfilled deductions` exceeding real `stock_quantity` — i.e., no overselling — verified by running high-concurrency randomized test batches against a known starting stock count and asserting the final reserved/confirmed total never exceeds it, across repeated runs to catch flaky race conditions rather than relying on a single passing run.

These two concurrency suites are run in CI on every change that touches the Cart, Orders, or Inventory modules, not just periodically, given how central these invariants are to the product's integrity.
