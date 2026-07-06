import { api } from './client'

/** Mirrors the ORDER WORKFLOW stages from the product spec, in order. */
export const ORDER_STAGES = [
  'PLACED',
  'PAYMENT_CONFIRMED',
  'ACCEPTED_BY_STORE',
  'PICKING',
  'SUBSTITUTION_APPROVAL',
  'PACKING',
  'READY_FOR_PICKUP',
  'DELIVERY_PARTNER_ASSIGNED',
  'PICKED_UP',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
] as const

export type OrderStage = (typeof ORDER_STAGES)[number] | 'CANCELLED'

/** Alias for the backend's `OrderStatus` enum — same values as `OrderStage`. */
export type OrderStatus = OrderStage

export const ORDER_STAGE_LABELS: Record<OrderStage, string> = {
  PLACED: 'Placed',
  PAYMENT_CONFIRMED: 'Payment confirmed',
  ACCEPTED_BY_STORE: 'Accepted by store',
  PICKING: 'Picking your items',
  SUBSTITUTION_APPROVAL: 'Substitution approval',
  PACKING: 'Packing',
  READY_FOR_PICKUP: 'Ready for pickup',
  DELIVERY_PARTNER_ASSIGNED: 'Delivery partner assigned',
  PICKED_UP: 'Picked up',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export type FulfilmentType = 'IMMEDIATE' | 'SCHEDULED' | 'PICKUP'

/**
 * UI-only selector today: the backend's `CheckoutRequest` is just `{ branchId }` and
 * does not yet accept a payment method, so this is not sent to the API.
 */
export type PaymentMethod = 'CARD' | 'UPI' | 'COD'

/** Matches `OrderResponse.java`'s `items[]` shape exactly. */
export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

/** Matches `OrderResponse.java` exactly — no client-only fields. */
export interface Order {
  id: number
  supermarketId: number
  branchId: number
  status: OrderStatus
  totalAmount: number
  currency: string
  items: OrderItem[]
  deliveryAddress: string | null
  createdAt: string
}

export type PaymentProvider = 'MOCK' | 'RAZORPAY'

/** Matches `CheckoutResponse.payment` (`PaymentIntentResponse`) exactly. */
export interface PaymentIntent {
  provider: PaymentProvider
  requiresClientAction: boolean
  gatewayOrderId: string | null
  providerKeyId: string | null
  amountMinorUnits: number
  currency: string
}

export interface CheckoutResponse {
  order: Order
  payment: PaymentIntent
}

/** Body of `POST /api/checkout/{orderId}/payment/verify`. */
export interface PaymentVerificationPayload {
  gatewayOrderId?: string
  gatewayPaymentId?: string
  gatewaySignature?: string
}

export const ordersApi = {
  /**
   * `POST /api/checkout` — reserves inventory, creates the order + a pending payment,
   * and returns a payment intent the caller must branch on (`requiresClientAction`).
   * `idempotencyKey` must be generated client-side and reused across retries of the
   * same checkout attempt.
   */
  checkout: (branchId: number, idempotencyKey: string, addressId?: number) =>
    api.post<CheckoutResponse>('/checkout', { branchId, addressId }, { idempotencyKey }),

  /** `POST /api/checkout/{orderId}/payment/verify` — idempotent; returns the updated order. */
  verifyPayment: (orderId: number, payload: PaymentVerificationPayload, idempotencyKey: string) =>
    api.post<Order>(`/checkout/${orderId}/payment/verify`, payload, { idempotencyKey }),

  listMine: () => api.get<Order[]>('/orders'),

  getById: (orderId: number) => api.get<Order>(`/orders/${orderId}`),

  getStatus: (orderId: number) => api.get<{ status: OrderStatus }>(`/orders/${orderId}/status`),
}
