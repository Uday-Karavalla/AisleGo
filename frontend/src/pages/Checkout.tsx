import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { addressesApi } from '../api/addresses'
import type { Address, NewAddress } from '../api/addresses'
import { ordersApi } from '../api/orders'
import type { FulfilmentType, PaymentMethod } from '../api/orders'
import { ApiError } from '../api/client'
import { createMockOrder } from '../api/mockOrders'
import { MapPinIcon, ClockIcon, CheckIcon } from '../components/icons'

const IDEMPOTENCY_STORAGE_KEY = 'aislego.checkoutIdempotencyKey'

/** Generated once per checkout attempt and reused across retries so a place-order
 *  retry (e.g. after a flaky network call) never double-charges or double-creates an order. */
function getOrCreateIdempotencyKey(): string {
  const existing = sessionStorage.getItem(IDEMPOTENCY_STORAGE_KEY)
  if (existing) return existing
  const generated = crypto.randomUUID()
  sessionStorage.setItem(IDEMPOTENCY_STORAGE_KEY, generated)
  return generated
}

const FULFILMENT_OPTIONS: { value: FulfilmentType; label: string }[] = [
  { value: 'IMMEDIATE', label: 'ASAP' },
  { value: 'SCHEDULED', label: 'Schedule' },
  { value: 'PICKUP', label: 'Pickup' },
]

const PAYMENT_OPTIONS: { value: PaymentMethod; label: string }[] = [
  { value: 'UPI', label: 'UPI' },
  { value: 'CARD', label: 'Card' },
  { value: 'COD', label: 'Cash' },
]

export default function Checkout() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { cart, isEmpty, clearCart } = useCart()
  const [, setLastOrderId] = useLocalStorage<string | null>('aislego.lastOrderId', null)

  const [addresses, setAddresses] = useState<Address[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null)
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [addressDraft, setAddressDraft] = useState<NewAddress>({
    label: 'Home',
    line1: '',
    city: '',
    state: '',
    postalCode: '',
  })

  const [fulfilmentType, setFulfilmentType] = useState<FulfilmentType>('IMMEDIATE')
  const [scheduledFor, setScheduledFor] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('UPI')

  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [idempotencyKey] = useState(getOrCreateIdempotencyKey)

  useEffect(() => {
    addressesApi
      .list()
      .then((list) => {
        setAddresses(list)
        const preferred = list.find((address) => address.isDefault) ?? list[0]
        if (preferred) setSelectedAddressId(preferred.id)
      })
      .catch(() => {})
  }, [])

  if (isEmpty) {
    return <Navigate to="/cart" replace />
  }

  if (user && !user.emailVerified) {
    return (
      <div className="flex flex-col items-center gap-4 px-5 py-16 text-center">
        <h1 className="text-lg font-bold text-ink">Please verify your email first</h1>
        <p className="text-sm text-ink-muted">
          To keep orders genuine, you need to verify your email before placing one.
        </p>
        <Link to="/verify-email" className="btn-primary">
          Verify email
        </Link>
      </div>
    )
  }

  async function handleAddAddress(event: FormEvent) {
    event.preventDefault()
    if (!addressDraft.line1 || !addressDraft.city || !addressDraft.postalCode) return
    const created = await addressesApi.create(addressDraft)
    setAddresses((prev) => [...prev, created])
    setSelectedAddressId(created.id)
    setShowAddressForm(false)
  }

  function finishCheckout(orderId: number) {
    sessionStorage.removeItem(IDEMPOTENCY_STORAGE_KEY)
    setLastOrderId(String(orderId))
    clearCart()
    navigate(`/orders/${orderId}`)
  }

  // NOTE: `fulfilmentType`/`scheduledFor`/`couponCode`/`paymentMethod` are still UI-only
  // state — the backend doesn't yet accept any of these, so they are intentionally not
  // sent to the API. `addressId` (below) is now real: it's snapshotted onto the order.
  async function handlePlaceOrder() {
    if (!cart.storeId) return
    if (fulfilmentType !== 'PICKUP' && !selectedAddressId) {
      setErrorMessage('Please select or add a delivery address.')
      return
    }

    const branchId = Number(cart.storeId)
    const addressId =
      fulfilmentType !== 'PICKUP' && selectedAddressId ? Number(selectedAddressId) : undefined

    setSubmitting(true)
    setErrorMessage(null)

    try {
      const { order, payment } = await ordersApi.checkout(branchId, idempotencyKey, addressId)

      if (!payment.requiresClientAction) {
        // Mock provider: nothing to collect from the shopper, verify immediately.
        await ordersApi.verifyPayment(order.id, {}, idempotencyKey)
        finishCheckout(order.id)
        return
      }

      // Razorpay provider: open the hosted widget; it collects card/UPI details
      // directly with Razorpay, so no payment details ever touch our backend.
      if (typeof window.Razorpay !== 'function') {
        setErrorMessage('Payment widget failed to load. Please check your connection and try again.')
        setSubmitting(false)
        return
      }

      const razorpay = new window.Razorpay({
        key: payment.providerKeyId!,
        order_id: payment.gatewayOrderId!,
        amount: payment.amountMinorUnits,
        currency: payment.currency,
        name: 'AisleGo',
        handler: async (razorpayResponse) => {
          try {
            await ordersApi.verifyPayment(
              order.id,
              {
                gatewayOrderId: razorpayResponse.razorpay_order_id,
                gatewayPaymentId: razorpayResponse.razorpay_payment_id,
                gatewaySignature: razorpayResponse.razorpay_signature,
              },
              idempotencyKey,
            )
            finishCheckout(order.id)
          } catch (verifyError) {
            setErrorMessage(
              verifyError instanceof Error
                ? verifyError.message
                : 'We could not confirm your payment. Please try again.',
            )
            setSubmitting(false)
          }
        },
        modal: {
          ondismiss: () => {
            // Shopper closed the widget without paying — not a hard error, just let them retry.
            setSubmitting(false)
            setErrorMessage('Payment cancelled. You can try again when ready.')
          },
        },
      })
      razorpay.open()
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        // No backend yet: simulate the order locally so tracking still works end to end.
        const mockOrder = createMockOrder({ branchId, cart })
        finishCheckout(mockOrder.id)
        return
      }
      setErrorMessage(error instanceof Error ? error.message : 'Could not place your order. Please try again.')
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6 px-5 py-6">
      <h1 className="text-xl font-extrabold text-ink">Checkout</h1>

      <section className="card flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-sm font-bold text-ink">
            <MapPinIcon className="h-4 w-4 text-brand-600" />
            Delivery address
          </h2>
          <Link to="/addresses" className="text-xs font-semibold text-brand-700">
            Manage
          </Link>
        </div>

        {fulfilmentType === 'PICKUP' ? (
          <p className="text-sm text-ink-muted">
            You&apos;ll collect this order from {cart.storeName}. No delivery address needed.
          </p>
        ) : (
          <>
            {addresses.length === 0 && !showAddressForm && (
              <p className="text-sm text-ink-muted">No saved addresses yet.</p>
            )}

            <div className="flex flex-col gap-2">
              {addresses.map((address) => (
                <label
                  key={address.id}
                  className={`flex cursor-pointer items-start gap-3 rounded-2xl border-2 p-3 text-sm ${
                    selectedAddressId === address.id ? 'border-brand-600 bg-brand-50' : 'border-black/10'
                  }`}
                >
                  <input
                    type="radio"
                    name="address"
                    className="mt-1"
                    checked={selectedAddressId === address.id}
                    onChange={() => setSelectedAddressId(address.id)}
                  />
                  <span>
                    <span className="block font-semibold text-ink">{address.label}</span>
                    <span className="block text-ink-muted">
                      {address.line1}, {address.city} {address.postalCode}
                    </span>
                  </span>
                </label>
              ))}
            </div>

            {showAddressForm ? (
              <form onSubmit={handleAddAddress} className="flex flex-col gap-2 rounded-2xl bg-surface-muted p-3">
                <input
                  className="input-field"
                  placeholder="Label (e.g. Home)"
                  value={addressDraft.label}
                  onChange={(event) => setAddressDraft((draft) => ({ ...draft, label: event.target.value }))}
                />
                <input
                  className="input-field"
                  placeholder="Address line"
                  value={addressDraft.line1}
                  onChange={(event) => setAddressDraft((draft) => ({ ...draft, line1: event.target.value }))}
                />
                <div className="flex gap-2">
                  <input
                    className="input-field"
                    placeholder="City"
                    value={addressDraft.city}
                    onChange={(event) => setAddressDraft((draft) => ({ ...draft, city: event.target.value }))}
                  />
                  <input
                    className="input-field"
                    placeholder="PIN code"
                    value={addressDraft.postalCode}
                    onChange={(event) => setAddressDraft((draft) => ({ ...draft, postalCode: event.target.value }))}
                  />
                </div>
                <input
                  className="input-field"
                  placeholder="State"
                  value={addressDraft.state}
                  onChange={(event) => setAddressDraft((draft) => ({ ...draft, state: event.target.value }))}
                />
                <div className="flex gap-2">
                  <button type="submit" className="btn-primary flex-1 py-2.5 text-sm">
                    Save address
                  </button>
                  <button type="button" className="btn-ghost" onClick={() => setShowAddressForm(false)}>
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <button type="button" className="btn-ghost self-start" onClick={() => setShowAddressForm(true)}>
                + Add new address
              </button>
            )}
          </>
        )}
      </section>

      <section className="card flex flex-col gap-3">
        <h2 className="flex items-center gap-2 text-sm font-bold text-ink">
          <ClockIcon className="h-4 w-4 text-brand-600" />
          Fulfilment
        </h2>
        <div className="grid grid-cols-3 gap-2">
          {FULFILMENT_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => setFulfilmentType(option.value)}
              aria-pressed={fulfilmentType === option.value}
              className={`rounded-2xl border-2 py-2.5 text-xs font-semibold ${
                fulfilmentType === option.value
                  ? 'border-brand-600 bg-brand-50 text-brand-700'
                  : 'border-black/10 text-ink-muted'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
        {fulfilmentType === 'SCHEDULED' && (
          <input
            type="datetime-local"
            className="input-field"
            value={scheduledFor}
            onChange={(event) => setScheduledFor(event.target.value)}
          />
        )}
      </section>

      <section className="card flex flex-col gap-3">
        {/* Display-only for now: the backend doesn't accept a payment method choice yet
            (it picks Mock vs. Razorpay server-side), so this selection isn't sent anywhere. */}
        <h2 className="text-sm font-bold text-ink">Payment method</h2>
        <div className="grid grid-cols-3 gap-2">
          {PAYMENT_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => setPaymentMethod(option.value)}
              aria-pressed={paymentMethod === option.value}
              className={`rounded-2xl border-2 py-2.5 text-xs font-semibold ${
                paymentMethod === option.value
                  ? 'border-brand-600 bg-brand-50 text-brand-700'
                  : 'border-black/10 text-ink-muted'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </section>

      <section className="card flex flex-col gap-2">
        <div className="flex justify-between text-sm text-ink-muted">
          <span>Subtotal</span>
          <span>₹{cart.subtotal.toFixed(0)}</span>
        </div>
        <div className="flex justify-between text-sm text-ink-muted">
          <span>Delivery fee</span>
          <span>₹{cart.deliveryFee.toFixed(0)}</span>
        </div>
        {cart.discount > 0 && (
          <div className="flex justify-between text-sm text-brand-700">
            <span>Discount</span>
            <span>-₹{cart.discount.toFixed(0)}</span>
          </div>
        )}
        <div className="flex justify-between border-t border-black/5 pt-2 text-base font-bold text-ink">
          <span>Total</span>
          <span>₹{cart.total.toFixed(0)}</span>
        </div>
      </section>

      {errorMessage && (
        <p role="alert" className="text-center text-sm text-danger-500">
          {errorMessage}
        </p>
      )}

      <button type="button" onClick={handlePlaceOrder} disabled={submitting} className="btn-primary">
        {submitting ? (
          'Placing order…'
        ) : (
          <>
            <CheckIcon className="h-5 w-5" />
            Pay &amp; place order
          </>
        )}
      </button>
    </div>
  )
}
