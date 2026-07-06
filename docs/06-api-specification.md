# AisleGo — API Specification

**Related docs:** `04-architecture.md`, `05-database-er-diagram.md`, `08-security-and-fraud-control.md`

All endpoints are versioned under `/api/v1`. Authentication is via `Authorization: Bearer <JWT>` unless marked **Public**. "Role required" reflects the minimum role; scope checks (own store/branch/self) are applied on top per `02-roles-and-permissions.md`.

---

## 1. Identity & Auth Module — `/api/v1/auth`, `/api/v1/users`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register a new customer or delivery-partner account |
| POST | `/auth/login` | Public | Authenticate with credentials, returns access + refresh token pair |
| POST | `/auth/otp/request` | Public | Request an OTP for phone-based login/verification |
| POST | `/auth/otp/verify` | Public | Verify OTP and complete login |
| POST | `/auth/refresh` | Public (valid refresh token) | Exchange a refresh token for a new access token |
| POST | `/auth/logout` | Any authenticated | Revoke the current refresh token |
| GET | `/users/me` | Any authenticated | Get the current user's profile |
| PATCH | `/users/me` | Any authenticated | Update own profile |
| POST | `/users/me/addresses` | Customer | Add a delivery address |
| GET | `/users/me/addresses` | Customer | List saved addresses |
| PATCH | `/users/me/addresses/{addressId}` | Customer (own) | Update an address |
| DELETE | `/users/me/addresses/{addressId}` | Customer (own) | Remove an address |
| POST | `/staff` | Supermarket Owner | Create a staff account (branch manager/picker) |
| PATCH | `/staff/{staffId}` | Owner / Branch Manager (scoped) | Update staff role/permissions or deactivate |
| GET | `/staff` | Owner / Branch Manager (scoped) | List staff for own store/branch |

## 2. Catalogue Module — `/api/v1/supermarkets`, `/api/v1/categories`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| GET | `/supermarkets/nearby` | Customer | Discover serviceable supermarkets near a lat/long. Response now includes real `etaMinutes`/`isOpen` fields, backed by the opt-in OpenRouteService `RoutingService` provider (real driving distance/ETA) or the default Haversine provider (great-circle estimate) |
| GET | `/stores/geocode` | Customer | Resolve a free-text address into `{lat, lng}` via the configured `RoutingService` provider; `404` if the provider doesn't support geocoding (default `haversine`) or the address can't be found (opt-in `openrouteservice`) |
| GET | `/supermarkets/{supermarketId}` | Public/Customer | Get storefront details (hours, rating, branches) |
| POST | `/supermarkets` | Public (creates PENDING record) | Submit new supermarket registration |
| PATCH | `/supermarkets/{supermarketId}` | Owner (own store) | Update store profile info |
| POST | `/supermarkets/{supermarketId}/verify` | Platform Admin | Approve/reject verification |
| POST | `/supermarkets/{supermarketId}/branches` | Owner (own store) | Create a branch |
| GET | `/supermarkets/{supermarketId}/branches` | Owner / Admin | List branches |
| PATCH | `/branches/{branchId}` | Owner / Branch Manager (own branch) | Update branch details, hours, delivery area |
| GET | `/supermarkets/{supermarketId}/products` | Public/Customer | Browse/search a store's catalogue (query params: `q`, `categoryId`, `minPrice`, `maxPrice`, `inStockOnly`) |
| GET | `/products/{productId}` | Public/Customer | Product detail |
| POST | `/supermarkets/{supermarketId}/products` | Owner / Branch Manager | Add a product (manual) |
| POST | `/supermarkets/{supermarketId}/products/import` | Owner | Bulk import via CSV/Excel |
| POST | `/products/scan` | Owner / Branch Manager | Resolve/create a product from a scanned barcode |
| PATCH | `/products/{productId}` | Owner / Branch Manager (scoped) | Update price, description, category, image |
| DELETE | `/products/{productId}` | Owner | Deactivate a product |
| GET | `/categories` | Public | List global category taxonomy |
| POST | `/categories` | Platform Admin | Create/manage global categories |
| POST | `/supermarkets/{supermarketId}/coupons` | Owner | Create a store coupon/offer/bundle |
| GET | `/supermarkets/{supermarketId}/coupons` | Owner / Customer (active only) | List active offers for a store |
| POST | `/admin/promotions` | Platform Admin | Create platform-wide featured promotions |

## 3. Inventory Module — `/api/v1/branches/{branchId}/inventory`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| GET | `/branches/{branchId}/inventory` | Owner / Branch Manager (own branch) | List stock levels for a branch |
| PATCH | `/branches/{branchId}/inventory/{productId}` | Owner / Branch Manager (own branch) | Adjust stock quantity / reorder threshold |
| POST | `/branches/{branchId}/inventory/{productId}/reserve` | Internal (called by Orders module) | Reserve stock for an in-flight checkout |
| POST | `/branches/{branchId}/inventory/{productId}/release` | Internal (called by Orders module) | Release a reservation (payment failed/timed out) |
| POST | `/branches/{branchId}/inventory/{productId}/confirm` | Internal (called by Orders module) | Convert a reservation into a confirmed deduction |

## 4. Orders / Cart / Checkout Module — `/api/v1/cart`, `/api/v1/orders`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| GET | `/cart` | Customer | View current cart |
| POST | `/cart/items` | Customer | Add a product to cart (**409 if from a different supermarket** — see §6.1) |
| PATCH | `/cart/items/{cartItemId}` | Customer (own cart) | Update quantity / substitution preference |
| DELETE | `/cart/items/{cartItemId}` | Customer (own cart) | Remove an item |
| DELETE | `/cart` | Customer (own cart) | Clear the entire cart |
| POST | `/cart/coupon` | Customer | Apply a coupon code to the cart |
| DELETE | `/cart/coupon` | Customer | Remove applied coupon |
| POST | `/checkout` | Customer | Validate cart, reserve stock, create payment intent, place order (body: `{branchId}`) |
| POST | `/checkout/{orderId}/payment/verify` | Customer (own) | Verify the gateway payment callback and confirm or cancel the order (idempotent) |
| GET | `/orders` | Customer / Owner / Branch Manager (scoped) | List orders (own orders, or own store's/branch's orders) |
| GET | `/orders/{orderId}` | Customer (own) / Owner / Branch Manager / Admin (scoped) | Order detail + full status timeline |
| POST | `/orders/{orderId}/accept` | Branch Manager (own branch) | Accept an incoming order |
| POST | `/orders/{orderId}/reject` | Branch Manager (own branch) | Reject an incoming order |
| POST | `/orders/{orderId}/assign-staff` | Branch Manager (own branch) | Assign order to a picker |
| PATCH | `/orders/{orderId}/items/{itemId}/pick` | Picker (assigned) | Mark item picked / out-of-stock |
| POST | `/orders/{orderId}/items/{itemId}/substitute` | Picker / Branch Manager | Suggest a substitution |
| POST | `/orders/{orderId}/items/{itemId}/substitute/approve` | Customer / Branch Manager | Approve or reject a suggested substitution |
| POST | `/orders/{orderId}/ready` | Picker / Branch Manager (own branch) | Mark order ready for pickup |
| POST | `/orders/{orderId}/cancel` | Customer (own, pre-fulfillment) / Branch Manager | Cancel an order |
| POST | `/orders/{orderId}/repeat` | Customer (own) | Recreate a cart from a past order |
| GET | `/orders/{orderId}/invoice` | Customer (own) / Owner (own store) | Download invoice PDF |
| POST | `/orders/{orderId}/report-issue` | Customer (own) | Report missing/damaged product |
| POST | `/orders/{orderId}/reviews` | Customer (own, delivered order) | Submit store/product/delivery ratings |

## 5. Payments Module — `/api/v1/payments`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| POST | `/checkout` | Customer (see §4) | Payment intent creation happens inline here, not as a separate call — see the checkout flow below |
| POST | `/payments/webhook/razorpay` | Public (Razorpay-signed) | Server-to-server payment confirmation from Razorpay, verified via `X-Razorpay-Signature` |
| GET | `/payments/{paymentId}` | Customer (own) / Owner / Admin | Payment status/detail |
| POST | `/payments/{paymentId}/refund` | Branch Manager (within threshold) / Admin (above threshold) | Issue a full or partial refund |
| GET | `/supermarkets/{supermarketId}/settlements` | Owner / Admin | View settlement statements |

## 6. Delivery Module — `/api/v1/delivery`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| POST | `/delivery-partners/register` | Public | Delivery-partner registration |
| POST | `/delivery-partners/{id}/verify` | Platform Admin | Approve/reject verification |
| PATCH | `/delivery-partners/me/availability` | Delivery Partner | Toggle online/offline |
| PATCH | `/delivery-partners/me/location` | Delivery Partner | Push live location update |
| GET | `/delivery-partners/me/opportunities` | Delivery Partner | List nearby available assignments |
| POST | `/deliveries/{deliveryId}/accept` | Delivery Partner | Accept an assignment |
| POST | `/deliveries/{deliveryId}/reject` | Delivery Partner | Reject/pass on an assignment |
| POST | `/deliveries/{deliveryId}/pickup-otp/verify` | Delivery Partner / Branch staff | Verify pickup OTP/QR |
| POST | `/deliveries/{deliveryId}/delivery-otp/verify` | Delivery Partner | Verify delivery OTP |
| GET | `/delivery-partners/me/earnings` | Delivery Partner | Earnings and delivery history |

## 7. Loyalty Module — `/api/v1/loyalty`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| GET | `/loyalty/me` | Customer | View points balance and transaction history |
| POST | `/loyalty/redeem` | Customer | Redeem points against an active cart |

## 8. Administration Module — `/api/v1/admin`

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| GET | `/admin/supermarkets/pending` | Platform Admin | Queue of pending store verifications |
| GET | `/admin/delivery-partners/pending` | Platform Admin | Queue of pending partner verifications |
| GET | `/admin/disputes` | Platform Admin | List open disputes/complaints |
| GET | `/admin/disputes/{disputeId}` | Platform Admin | Dispute detail with full order/audit context |
| POST | `/admin/disputes/{disputeId}/resolve` | Platform Admin | Resolve a dispute, trigger refund/annotation |
| PATCH | `/admin/accounts/{accountId}/suspend` | Platform Admin | Suspend a fraudulent/violating account |
| GET | `/admin/analytics/platform` | Platform Admin | Platform-wide GMV/order/delivery analytics |
| GET | `/admin/audit-log` | Platform Admin | Query the platform audit log |
| PATCH | `/admin/config/commissions` | Platform Admin | Configure commission rates and subscription tiers |

---

## 9. First Working Flow — OpenAPI 3.0 Slice

The following is a realistic OpenAPI excerpt covering exactly the "first working flow": **store discovery → catalogue browse → cart → checkout → payment verification → order status**. It deliberately encodes the single-supermarket-cart rule as a documented `409 Conflict` response, and the checkout/payment slice as the two-phase flow described in `08-security-and-fraud-control.md` §2 (order placement never itself handles card/UPI data).

```yaml
openapi: 3.0.3
info:
  title: AisleGo API — First Working Flow
  description: >
    Store discovery through order placement and status tracking.
    This slice is the Phase 0 "golden path" API surface.
  version: "1.0.0"
servers:
  - url: https://api.aislego.com/api/v1
    description: Production
  - url: https://staging-api.aislego.com/api/v1
    description: Staging

paths:
  /supermarkets/nearby:
    get:
      summary: Discover nearby serviceable supermarkets
      tags: [Catalogue]
      security:
        - bearerAuth: []
      parameters:
        - name: lat
          in: query
          required: true
          schema: { type: number, format: double }
        - name: lng
          in: query
          required: true
          schema: { type: number, format: double }
        - name: radiusKm
          in: query
          schema: { type: number, default: 5 }
      responses:
        '200':
          description: List of nearby supermarkets
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/SupermarketSummary' }

  /supermarkets/{supermarketId}/products:
    get:
      summary: Browse/search a supermarket's catalogue
      tags: [Catalogue]
      security:
        - bearerAuth: []
      parameters:
        - name: supermarketId
          in: path
          required: true
          schema: { type: string, format: uuid }
        - name: q
          in: query
          schema: { type: string }
        - name: categoryId
          in: query
          schema: { type: string, format: uuid }
        - name: inStockOnly
          in: query
          schema: { type: boolean, default: false }
        - name: page
          in: query
          schema: { type: integer, default: 0 }
      responses:
        '200':
          description: Paginated product list
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items: { $ref: '#/components/schemas/Product' }
                  page: { type: integer }
                  totalPages: { type: integer }

  /cart/items:
    post:
      summary: Add a product to the customer's cart
      description: >
        Fails with 409 if the cart already contains items from a different
        supermarket. AisleGo enforces one order = one supermarket; a customer
        must clear the cart or complete/cancel the existing cart before
        shopping at a different store.
      tags: [Orders]
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/AddCartItemRequest' }
      responses:
        '200':
          description: Item added
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Cart' }
        '409':
          description: Cart already scoped to a different supermarket
          content:
            application/json:
              schema: { $ref: '#/components/schemas/CrossStoreCartError' }
              example:
                code: CROSS_STORE_CART_CONFLICT
                message: >
                  Your cart contains items from "Green Valley Supermarket".
                  Clear your cart to add items from "FreshMart Express".
                existingSupermarketId: "b6e6a1b2-9e2a-4c39-9a3a-3a2f8e1a1111"
                existingSupermarketName: "Green Valley Supermarket"
                requestedSupermarketId: "f1a2c3d4-5678-4abc-9def-0123456789ab"
                requestedSupermarketName: "FreshMart Express"
        '422':
          description: Requested quantity exceeds available stock
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiError' }

  /checkout:
    post:
      summary: Validate cart, reserve inventory, create a payment intent, and place the order
      description: >
        Body is deliberately minimal (`{branchId}`) — delivery address,
        fulfilment type, and coupon selection are UI-only state in this
        phase and are not yet part of the checkout contract. The response
        bundles the placed order together with a gateway payment intent;
        the client then completes payment out-of-band with the gateway
        (for `razorpay`) or immediately (for `mock`) before calling
        `/checkout/{orderId}/payment/verify`.
      tags: [Orders]
      security:
        - bearerAuth: []
      parameters:
        - name: Idempotency-Key
          in: header
          required: true
          schema: { type: string }
          description: Client-generated key; safe to retry the same checkout attempt.
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/CheckoutRequest' }
      responses:
        '201':
          description: Order placed (status `PLACED`) and payment intent created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/CheckoutResponse' }
        '409':
          description: >
            Cart failed re-validation at checkout time — e.g. price/stock
            changed since it was added, or (defense-in-depth) a cross-store
            item was somehow present.
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiError' }
        '422':
          description: One or more items are now out of stock
          content:
            application/json:
              schema: { $ref: '#/components/schemas/StockValidationError' }

  /checkout/{orderId}/payment/verify:
    post:
      summary: Verify a gateway payment callback and confirm or cancel the order
      description: >
        Idempotent: if the order has already reached a terminal payment
        state (`PAYMENT_CONFIRMED` or `CANCELLED`) it is returned as-is
        with no re-verification. For the `mock` provider this always
        succeeds; for `razorpay` the signature is verified server-side
        via `razorpay-java`'s `Utils.verifyPaymentSignature` (see
        `08-security-and-fraud-control.md` §2). On a failed/invalid
        signature the order is cancelled and its reserved inventory is
        released back to the branch.
      tags: [Orders]
      security:
        - bearerAuth: []
      parameters:
        - name: orderId
          in: path
          required: true
          schema: { type: string, format: uuid }
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/PaymentVerificationRequest' }
      responses:
        '200':
          description: >
            Order after verification — status is `PAYMENT_CONFIRMED` on
            success or `CANCELLED` on a failed/invalid signature.
          content:
            application/json:
              schema: { $ref: '#/components/schemas/OrderDetail' }
        '404':
          description: Order not found, or not owned by the calling customer
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiError' }

  /payments/webhook/razorpay:
    post:
      summary: Razorpay server-to-server payment webhook
      description: >
        Public endpoint called directly by Razorpay's servers, never by
        the AisleGo frontend. Verifies `X-Razorpay-Signature` against
        `RAZORPAY_WEBHOOK_SECRET` via `Utils.verifyWebhookSignature`,
        resolves the `Payment` by the gateway order id in the event
        payload, then runs the same idempotent capture logic as
        `/checkout/{orderId}/payment/verify` — a defense-in-depth path
        for customers who close the browser before the client-side
        verify call fires. See `08-security-and-fraud-control.md` §2
        for the signature-verification design.
      tags: [Payments]
      security: []
      parameters:
        - name: X-Razorpay-Signature
          in: header
          required: true
          schema: { type: string }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              description: >
                Razorpay webhook event payload (opaque here; see
                Razorpay's own webhook payload reference for the full
                shape). AisleGo reads only the payment/order identifiers
                needed to resolve the corresponding `Payment` row.
      responses:
        '200':
          description: Webhook processed (idempotent no-op if already handled by the client-side verify call)
        '400':
          description: Missing or invalid signature
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiError' }

  /orders/{orderId}:
    get:
      summary: Get order detail and status timeline
      tags: [Orders]
      security:
        - bearerAuth: []
      parameters:
        - name: orderId
          in: path
          required: true
          schema: { type: string, format: uuid }
      responses:
        '200':
          description: Order detail
          content:
            application/json:
              schema: { $ref: '#/components/schemas/OrderDetail' }
        '404':
          description: Order not found or not visible to this user
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ApiError' }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    SupermarketSummary:
      type: object
      properties:
        id: { type: string, format: uuid }
        displayName: { type: string, example: "Green Valley Supermarket" }
        rating: { type: number, format: float, example: 4.3 }
        distanceKm: { type: number, format: float, example: 1.8 }
        estimatedDeliveryMinutes: { type: integer, example: 35 }
        isOpen: { type: boolean }

    Product:
      type: object
      properties:
        id: { type: string, format: uuid }
        supermarketId: { type: string, format: uuid }
        name: { type: string, example: "Amul Toned Milk 500ml" }
        unit: { type: string, example: "500ml" }
        priceMinor: { type: integer, example: 2800 }
        currency: { type: string, example: "INR" }
        inStock: { type: boolean }
        imageUrl: { type: string, format: uri }

    AddCartItemRequest:
      type: object
      required: [productId, quantity]
      properties:
        productId: { type: string, format: uuid }
        quantity: { type: integer, minimum: 1, example: 2 }
        allowSubstitution: { type: boolean, default: true }

    Cart:
      type: object
      properties:
        id: { type: string, format: uuid }
        supermarketId: { type: string, format: uuid, nullable: true }
        items:
          type: array
          items: { $ref: '#/components/schemas/CartItem' }
        subtotalMinor: { type: integer }

    CartItem:
      type: object
      properties:
        id: { type: string, format: uuid }
        productId: { type: string, format: uuid }
        productName: { type: string }
        quantity: { type: integer }
        unitPriceMinor: { type: integer }
        allowSubstitution: { type: boolean }

    CrossStoreCartError:
      type: object
      properties:
        code: { type: string, example: CROSS_STORE_CART_CONFLICT }
        message: { type: string }
        existingSupermarketId: { type: string, format: uuid }
        existingSupermarketName: { type: string }
        requestedSupermarketId: { type: string, format: uuid }
        requestedSupermarketName: { type: string }

    CheckoutRequest:
      type: object
      required: [branchId]
      properties:
        branchId:
          type: string
          format: uuid
          description: >
            The branch the cart's items are reserved from. Delivery
            address, fulfilment type, and coupon selection are UI-only
            in this phase and are not yet sent to the backend.

    CheckoutResponse:
      type: object
      properties:
        order:
          $ref: '#/components/schemas/OrderDetail'
        payment:
          $ref: '#/components/schemas/PaymentIntentResponse'

    PaymentIntentResponse:
      type: object
      properties:
        provider:
          type: string
          enum: [mock, razorpay]
        requiresClientAction:
          type: boolean
          description: >
            `false` for `mock` (server-side confirmation only, no widget);
            `true` for `razorpay`, meaning the client must open the
            Razorpay Checkout widget before calling the verify endpoint.
        gatewayOrderId:
          type: string
          nullable: true
          description: Razorpay order id from their Orders API; `null` for `mock`.
        providerKeyId:
          type: string
          nullable: true
          description: Razorpay public key id used to initialize Checkout.js; `null` for `mock`.
        amountMinorUnits:
          type: integer
          example: 45900
        currency:
          type: string
          example: INR

    PaymentVerificationRequest:
      type: object
      description: >
        All fields optional. Sent empty for `mock` (which always
        succeeds); for `razorpay` these are populated from the
        Checkout.js success handler's `razorpay_order_id`/
        `razorpay_payment_id`/`razorpay_signature` and verified
        server-side against the HMAC signature.
      properties:
        gatewayOrderId:
          type: string
          nullable: true
        gatewayPaymentId:
          type: string
          nullable: true
        gatewaySignature:
          type: string
          nullable: true

    OrderDetail:
      type: object
      properties:
        id: { type: string, format: uuid }
        supermarketId: { type: string, format: uuid }
        branchId: { type: string, format: uuid }
        status:
          type: string
          enum:
            - PLACED
            - PAYMENT_CONFIRMED
            - ACCEPTED
            - PICKING
            - SUBSTITUTION_PENDING
            - PACKING
            - READY_FOR_PICKUP
            - PARTNER_ASSIGNED
            - PICKED_UP
            - OUT_FOR_DELIVERY
            - DELIVERED
            - CANCELLED
            - REFUNDED
        items:
          type: array
          items: { $ref: '#/components/schemas/OrderItem' }
        totalMinor: { type: integer }
        statusHistory:
          type: array
          items:
            type: object
            properties:
              status: { type: string }
              at: { type: string, format: date-time }

    OrderItem:
      type: object
      properties:
        productId: { type: string, format: uuid }
        productName: { type: string }
        quantity: { type: integer }
        unitPriceMinor: { type: integer }
        lineStatus:
          type: string
          enum: [PENDING, PICKED, SUBSTITUTED, UNAVAILABLE, REFUNDED]

    StockValidationError:
      type: object
      properties:
        code: { type: string, example: INSUFFICIENT_STOCK }
        unavailableItems:
          type: array
          items:
            type: object
            properties:
              productId: { type: string, format: uuid }
              requestedQuantity: { type: integer }
              availableQuantity: { type: integer }

    ApiError:
      type: object
      properties:
        code: { type: string }
        message: { type: string }
```

## 10. Cross-Cutting API Conventions

- **Idempotency:** `POST /checkout` requires a client-supplied `Idempotency-Key` header (payment intent creation happens inline in the same call, not as a separate request); the server persists the key alongside the resulting order/payment and returns the original result on retry rather than creating a duplicate (see `08-security-and-fraud-control.md` §5). `POST /checkout/{orderId}/payment/verify` is naturally idempotent instead — it returns the existing order unchanged if payment has already been confirmed or cancelled.
- **Error shape:** all non-2xx responses return the `ApiError` shape (`code`, `message`) at minimum, with endpoint-specific extensions (like `CrossStoreCartError`, `StockValidationError`) where more structured client handling is useful.
- **Pagination:** list endpoints use `page`/`size` query params and return `{ content, page, totalPages }`.
- **Scoping is implicit, never client-supplied:** endpoints never accept a `supermarketId`/`branchId` from the caller to determine *whose* data to return for scoped operations (e.g. "list my branch's orders") — scope is always resolved server-side from the authenticated user's JWT claims, preventing IDOR-style scope-escalation attempts.
