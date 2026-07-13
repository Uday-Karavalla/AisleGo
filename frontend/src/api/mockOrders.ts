// Dev-only fallback: when the real /api/checkout backend isn't reachable yet, Checkout and
// OrderTracking simulate an order locally (persisted to localStorage) so the full
// Placed -> ... -> Delivered flow stays demoable end to end. Never used once the
// backend responds successfully — every call here is only reached from an ApiError
// with isNetworkError === true.
import { ORDER_STAGES } from './orders'
import type { FulfilmentType, Order, OrderItem } from './orders'
import type { Cart } from './cart'

const STORAGE_PREFIX = 'aislego.mockOrder.'

// Monotonic tie-breaker so two mock orders created within the same millisecond
// (e.g. rapid retries) still get distinct numeric ids.
let mockOrderSequence = 0

function storageKey(orderId: number): string {
  return `${STORAGE_PREFIX}${orderId}`
}

function saveMockOrder(order: Order): void {
  try {
    localStorage.setItem(storageKey(order.id), JSON.stringify(order))
  } catch {
    // ignore persistence failures
  }
}

export function loadMockOrder(orderId: number): Order | null {
  try {
    const raw = localStorage.getItem(storageKey(orderId))
    return raw ? (JSON.parse(raw) as Order) : null
  } catch {
    return null
  }
}

/** Creates a locally-simulated, already-paid order (mirrors the mock payment gateway's
 *  always-succeeds behaviour) when the real `/api/checkout` + verify calls can't be reached. */
export function createMockOrder(params: {
  branchId: number
  cart: Cart
  fulfilmentType: FulfilmentType
  scheduledFor?: string
}): Order {
  const { branchId, cart, fulfilmentType, scheduledFor } = params
  const placedAt = new Date()

  const items: OrderItem[] = cart.items.map((item) => ({
    productId: Number(item.productId) || 0,
    productName: item.name,
    quantity: item.quantity,
    unitPrice: item.price,
    lineTotal: item.price * item.quantity,
  }))

  const order: Order = {
    id: Date.now() + mockOrderSequence++,
    supermarketId: branchId,
    branchId,
    status: 'PAYMENT_CONFIRMED',
    fulfilmentType,
    scheduledFor: scheduledFor ?? null,
    subtotal: cart.subtotal,
    deliveryFee: fulfilmentType === 'PICKUP' ? 0 : cart.deliveryFee,
    totalAmount: cart.subtotal - cart.discount + (fulfilmentType === 'PICKUP' ? 0 : cart.deliveryFee),
    currency: 'INR',
    couponCode: cart.couponCode ?? null,
    discountAmount: cart.discount,
    items,
    deliveryAddress: null,
    createdAt: placedAt.toISOString(),
  }

  saveMockOrder(order)
  return order
}

/** Advances a locally-simulated order by one workflow stage, for demo purposes only. */
export function advanceMockOrder(orderId: number): Order | null {
  const order = loadMockOrder(orderId)
  if (!order) return null
  if (order.status === 'CANCELLED') return order

  const sequence = ORDER_STAGES
  const currentIndex = sequence.indexOf(order.status as (typeof ORDER_STAGES)[number])
  if (currentIndex === -1 || currentIndex >= sequence.length - 1) return order

  const nextStage = sequence[currentIndex + 1]
  const updated: Order = { ...order, status: nextStage }
  saveMockOrder(updated)
  return updated
}
