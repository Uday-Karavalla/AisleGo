# AisleGo — Multi-Supermarket Marketplace

AisleGo connects existing local supermarkets, customers, and delivery partners. Unlike dark-store platforms, AisleGo does not run its own warehouses or control inventory — each supermarket keeps its own identity, pricing, inventory, and customer relationship. AisleGo is the discovery, ordering, and fulfillment-coordination layer on top.

**Core rule:** one order can only contain products from one supermarket. No cross-store carts.

See [`docs/`](docs/) for the full product and engineering documentation:

| Doc | Contents |
|---|---|
| [01-PRD](docs/01-PRD.md) | Problem statement, goals, roles, business rules |
| [02-roles-and-permissions](docs/02-roles-and-permissions.md) | Permission matrix for all 6 roles |
| [03-user-journeys](docs/03-user-journeys.md) | End-to-end flows per role |
| [04-architecture](docs/04-architecture.md) | Modular monolith design, module boundaries |
| [05-database-er-diagram](docs/05-database-er-diagram.md) | Full ER diagram + constraints |
| [06-api-specification](docs/06-api-specification.md) | Endpoint inventory + OpenAPI slice |
| [07-ui-screen-inventory](docs/07-ui-screen-inventory.md) | Screens per role + wireframes |
| [08-security-and-fraud-control](docs/08-security-and-fraud-control.md) | Auth, payments, OTPs, fraud signals |
| [09-roadmap](docs/09-roadmap.md) | Phase 0–3 delivery plan |
| [10-testing-strategy](docs/10-testing-strategy.md) | Test pyramid + invariant tests |
| [11-deployment-architecture](docs/11-deployment-architecture.md) | Local, CI, and cloud topology |

## What's implemented (Phase 0 — first working flow)

Store discovery → browse a store's catalogue → add to cart → checkout → order placed, with the single-supermarket-cart rule enforced **server-side**.

Deliberately out of scope for this phase (see [roadmap](docs/09-roadmap.md)): Kafka, OpenSearch, delivery-partner navigation, WebSocket live tracking, the delivery-partner and admin apps, CSV/barcode import, loyalty engine, analytics. These are stubbed with honest stand-ins:

- **Search** → Postgres `ILIKE` instead of OpenSearch
- **Nearby stores** → a real Haversine-vs-OpenRouteService `RoutingService` seam now exists (see [Maps & routing](#maps--routing) below); the default `HaversineRoutingService` still computes straight-line distance and an assumed-speed ETA with zero setup, which is what `docker compose up` uses out of the box
- **Payment** → a real Razorpay integration now exists (see [Payments](#payments) below), but it's opt-in; the default `MockPaymentGateway` synchronously "confirms" payment with zero setup, which is what `docker compose up` uses out of the box

## Stack

- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), PostgreSQL, Flyway — `backend/`
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, PWA — `frontend/`
- **Infra**: Docker Compose (Postgres, Redis, backend, frontend)

## Running it locally

Requires Docker Desktop. (The host machine only has Java 8 and no local Gradle — the backend is always built inside a JDK 21 Docker image, never on the bare host.)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Postgres: localhost:5432 (`aislego` / `aislego`)

Demo data (a few fictional supermarkets, branches, and products) is seeded automatically via Flyway (`backend/src/main/resources/db/migration/V2__seed_demo_data.sql`) so the flow is testable without building onboarding UI first.

### Backend only (outside Docker, if you have a local JDK 21)

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Frontend only (dev server, hot reload)

```bash
cd frontend
npm install
npm run dev
```

Set `frontend/.env` (copy from `.env.example`) if the backend isn't on `http://localhost:8080/api`. Note: with no backend running, the UI still works using bundled sample data as a fallback — useful for pure frontend iteration.

## Payments

Checkout uses a pluggable `PaymentService` with two providers:

- **`mock` (default)** — needs zero setup. This is what `docker compose up` uses out of the box: checkout creates a payment intent that always verifies successfully, so the golden path below works without any account or API keys.
- **`razorpay` (opt-in)** — a real integration against Razorpay's test-mode APIs and Checkout.js widget. Card/UPI details are entered directly into Razorpay's widget and never reach the AisleGo backend; the backend only ever sees an order id, a payment id, and a signature to verify (see `docs/08-security-and-fraud-control.md` §2 for the signature-verification design).

To try real Razorpay payments:

1. Create a free [Razorpay](https://razorpay.com/) account and switch to **Test Mode** — this only needs an email signup, no business verification is required for test-mode keys.
2. Grab the test-mode Key ID / Key Secret from the Razorpay dashboard.
3. Set these env vars before running `docker compose up`:
   ```bash
   PAYMENTS_PROVIDER=razorpay
   RAZORPAY_KEY_ID=...
   RAZORPAY_KEY_SECRET=...
   ```
4. Optionally, also set `RAZORPAY_WEBHOOK_SECRET=...` if you want to wire up the webhook (`POST /api/payments/webhook/razorpay`) from the Razorpay dashboard. Note that the webhook needs a **publicly reachable URL** to actually receive calls from Razorpay's servers (e.g. `ngrok http 8080` in dev) — this isn't demoable from this sandboxed setup, so the webhook path is implemented and unit-tested but not live-tested end-to-end.

## Maps & routing

Store discovery's distance/ETA calculation and manual address entry use a pluggable `RoutingService` with two providers:

- **`haversine` (default)** — needs zero setup. This is what `docker compose up` uses out of the box: distance is a straight-line (great-circle) calculation and ETA is approximated from an assumed average driving speed; this provider doesn't support geocoding, so manual address entry shows a clear "couldn't find that address" error instead.
- **`openrouteservice` (opt-in)** — a real integration against OpenRouteService's (OpenStreetMap-based) Matrix and Geocode APIs, giving real driving distance/ETA in store discovery and working manual-address search. On any API failure (network error, rate limit, outage) it falls back to the same Haversine calculation for that request, so discovery degrades gracefully instead of breaking.

To try real OpenRouteService routing:

1. Create a free [OpenRouteService](https://openrouteservice.org/dev/#/signup) account — free tier, no credit card required.
2. Grab your API key from the OpenRouteService dashboard.
3. Set these env vars before running `docker compose up`:
   ```bash
   ROUTING_PROVIDER=openrouteservice
   OPENROUTESERVICE_API_KEY=...
   ```

## Notifications

Order-lifecycle and supermarket-verification events (order placed, payment confirmed/failed, store approved/rejected) are delivered through a pluggable `NotificationService` with two providers, following the same pattern as Payments and Routing above:

- **`mock` (default)** — needs zero setup. This is what `docker compose up` uses out of the box: notifications are just logged (`LoggingNotificationService`) instead of actually sent, so the flows below are fully testable without any account or API keys.
- **`twilio` (opt-in)** — a real SMS integration against Twilio's REST API (no SDK dependency — a direct HTTP call, same spirit as the OpenRouteService integration above). A failed send is caught and logged rather than propagated, since a notification is a side-channel that must never break checkout or the admin review flow.

Not yet implemented: OTP delivery and delivery-partner notifications — both depend on functionality (OTP generation, the delivery-partner app/module) that doesn't exist yet (see [roadmap](docs/09-roadmap.md)).

To try real Twilio SMS:

1. Create a free [Twilio](https://www.twilio.com/try-twilio) trial account and verify a recipient number (trial accounts can only send to numbers you've verified).
2. Grab your Account SID, Auth Token, and trial phone number from the Twilio console.
3. Set these env vars before running `docker compose up`:
   ```bash
   NOTIFICATIONS_PROVIDER=twilio
   TWILIO_ACCOUNT_SID=...
   TWILIO_AUTH_TOKEN=...
   TWILIO_FROM_NUMBER=...
   ```

## Admin & supermarket onboarding

Supermarkets are no longer limited to Flyway seed data — a real self-registration + verification flow exists:

- **Supermarket owner self-registration** — a new owner registers at `/register-store` (or directly via `POST /api/auth/register-supermarket-owner`), which creates their user account and their supermarket in one step. The new store starts in `PENDING` status and is invisible to customer discovery (both the nearby-stores list and direct single-store lookup) until an admin approves it.
- **Seeded default admin account** — a dev-only admin user is seeded via Flyway (`admin@aislego.com` / `AisleGoAdmin!23`) so the verification workflow is demoable with zero setup, the same spirit as the mock payment gateway and Haversine routing above. **This is not a production credential** — it exists purely for local/demo use.
- **Admin review** — the admin logs in at `/login`, reviews pending stores at `/admin`, and approves or rejects each one (rejection requires a reason). Approving a store flips it to `VERIFIED` and it immediately becomes visible in customer discovery; rejecting sets `REJECTED` with the reason attached.
- **Owner status check** — the owner can check their application status at `/my-store` at any time, seeing `PENDING`, `VERIFIED`, or `REJECTED` (with the rejection reason, if any).

Delivery-partner verification is out of scope for now — it's deferred until the delivery-partner app/module exists (see [roadmap](docs/09-roadmap.md), Phase 1).

## Verifying the golden path

1. `docker compose up --build`
2. Open http://localhost:5173, allow (or deny, to test the manual-entry fallback) location access — with the default `haversine` provider, denying and typing an address manually now either resolves it via geocoding (with `openrouteservice` configured) or shows a clear error instead of silently using `0,0`
3. Pick a seeded demo supermarket from Store Discovery
4. Add a couple of products to the cart
5. Open a second seeded store and try adding one of its products — confirm the cross-store conflict dialog appears (this is enforced both client-side and server-side; the server returns `409 CROSS_STORE_CONFLICT`)
6. Proceed to checkout — this places the order and creates a payment intent (`POST /api/checkout`)
7. Confirm payment: with the default `mock` provider this happens automatically (the frontend immediately calls `POST /api/checkout/{orderId}/payment/verify` with an empty payload); with Razorpay configured, the Razorpay Checkout widget opens first and verify is called once you complete payment in it
8. Confirm the order lands in `PLACED` → `PAYMENT_CONFIRMED` and that seeded inventory was decremented (check via `psql` against the `postgres` container, or the `/api/stores/{id}/products` endpoint)

### Verifying store onboarding (optional)

1. Register a new store at `/register-store` (or `POST /api/auth/register-supermarket-owner`) — you're logged in immediately and land on `/my-store` showing `PENDING`.
2. Confirm the new store does **not** appear in `GET /api/stores/nearby` for its own coordinates, or in Store Discovery in the browser.
3. Log in as the seeded admin (`admin@aislego.com` / `AisleGoAdmin!23`) at `/login`, open `/admin`, and approve the new store.
4. Reload `/my-store` (as the owner) or `GET /api/stores/nearby` and confirm the store now shows `VERIFIED` and is visible in discovery.

## Tests

```bash
# Backend (needs a local JDK 21, or run inside the build container)
cd backend && ./gradlew test

# Frontend
cd frontend && npm run test
```
