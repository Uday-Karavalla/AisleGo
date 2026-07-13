import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useCart } from '../context/CartContext'
import { QuantityStepper } from '../components/QuantityStepper'
import { EmptyState } from '../components/EmptyState'
import { CartIcon, TagIcon, XIcon } from '../components/icons'
import { cartApi } from '../api/cart'
import type { AvailableCoupon } from '../api/cart'
import { getAuthToken } from '../api/client'

export default function Cart() {
  const navigate = useNavigate()
  const { cart, isEmpty, updateQuantity, setSubstitution, removeItem, applyCoupon } = useCart()
  const [couponInput, setCouponInput] = useState(cart.couponCode ?? '')
  const [couponSubmitting, setCouponSubmitting] = useState(false)
  const [couponError, setCouponError] = useState<string | null>(null)
  const [availableCoupons, setAvailableCoupons] = useState<AvailableCoupon[]>([])
  const [offersLoading, setOffersLoading] = useState(false)
  const signedIn = Boolean(getAuthToken())

  useEffect(() => {
    if (!signedIn || !cart.storeId || cart.items.length === 0) return
    let cancelled = false
    setOffersLoading(true)
    cartApi
      .availableCoupons()
      .then((offers) => {
        if (!cancelled) setAvailableCoupons(offers)
      })
      .catch(() => {
        if (!cancelled) setAvailableCoupons([])
      })
      .finally(() => {
        if (!cancelled) setOffersLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [signedIn, cart.storeId, cart.items.length, cart.subtotal])

  async function handleCouponSubmit(event: FormEvent) {
    event.preventDefault()
    setCouponSubmitting(true)
    setCouponError(null)
    try {
      await applyCoupon(couponInput)
    } catch (error) {
      setCouponError(error instanceof Error ? error.message : 'Could not apply that coupon.')
    } finally {
      setCouponSubmitting(false)
    }
  }

  async function applySuggestedCoupon(code: string) {
    setCouponInput(code)
    setCouponSubmitting(true)
    setCouponError(null)
    try {
      await applyCoupon(code)
    } catch (error) {
      setCouponError(error instanceof Error ? error.message : 'Could not apply that coupon.')
    } finally {
      setCouponSubmitting(false)
    }
  }

  if (isEmpty) {
    return (
      <EmptyState
        icon={<CartIcon className="h-12 w-12" />}
        title="Your cart is empty"
        description="Browse a nearby supermarket and add products to get started."
        action={
          <Link to="/stores" className="btn-primary">
            Find supermarkets
          </Link>
        }
      />
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-5 px-5 py-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">Ordering from</p>
        <h1 className="text-xl font-extrabold text-ink">{cart.storeName}</h1>
      </div>

      <ul className="flex flex-col gap-3">
        {cart.items.map((item) => (
          <li key={item.id} className="card flex flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
              <div>
                <p className="text-sm font-semibold text-ink">{item.name}</p>
                <p className="text-xs text-ink-faint">{item.unit}</p>
                <p className="mt-1 text-sm font-bold text-ink">₹{item.price.toFixed(0)}</p>
              </div>
              <button
                type="button"
                onClick={() => removeItem(item.id)}
                aria-label={`Remove ${item.name}`}
                className="rounded-full p-1.5 text-ink-faint transition active:scale-90 active:bg-black/5"
              >
                <XIcon className="h-4 w-4" />
              </button>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-2">
              <QuantityStepper
                size="sm"
                quantity={item.quantity}
                onIncrement={() => updateQuantity(item.id, item.quantity + 1)}
                onDecrement={() => updateQuantity(item.id, item.quantity - 1)}
                ariaLabel={`Quantity of ${item.name}`}
              />

              <div className="flex overflow-hidden rounded-full border border-black/10 text-xs font-semibold">
                <button
                  type="button"
                  aria-pressed={item.allowSubstitution}
                  onClick={() => setSubstitution(item.id, true)}
                  className={`px-3 py-1.5 ${item.allowSubstitution ? 'bg-brand-600 text-white' : 'text-ink-muted'}`}
                >
                  Allow swap
                </button>
                <button
                  type="button"
                  aria-pressed={!item.allowSubstitution}
                  onClick={() => setSubstitution(item.id, false)}
                  className={`px-3 py-1.5 ${!item.allowSubstitution ? 'bg-ink text-white' : 'text-ink-muted'}`}
                >
                  No swap
                </button>
              </div>
            </div>
          </li>
        ))}
      </ul>

      <div className="card flex flex-col gap-3">
        <label htmlFor="coupon" className="text-sm font-bold text-ink">
          Coupon code
        </label>
        <form className="flex gap-2" onSubmit={handleCouponSubmit}>
          <input
            id="coupon"
            className="input-field flex-1"
            placeholder="Enter coupon code"
            value={couponInput}
            onChange={(event) => setCouponInput(event.target.value)}
          />
          <button type="submit" className="btn-secondary px-4" disabled={couponSubmitting}>
            {couponSubmitting ? 'Applying…' : couponInput.trim() ? 'Apply' : 'Remove'}
          </button>
        </form>
        {couponError && <p role="alert" className="text-xs text-danger-500">{couponError}</p>}
        {cart.couponCode && <p className="text-xs text-brand-700">&ldquo;{cart.couponCode}&rdquo; applied.</p>}

        {!signedIn && (
          <div className="rounded-2xl bg-brand-50 p-3 text-sm text-brand-800">
            <p className="font-bold">Don&apos;t miss an offer</p>
            <p className="mt-0.5 text-xs text-brand-700">
              <Link to="/login" className="font-bold underline">Log in</Link> to reveal coupons available for this store.
            </p>
          </div>
        )}

        {signedIn && offersLoading && (
          <div className="h-20 animate-pulse rounded-2xl bg-black/5" aria-label="Loading available coupons" />
        )}

        {signedIn && !offersLoading && availableCoupons.some((offer) => offer.code !== cart.couponCode) && (
          <div className="border-t border-black/5 pt-3">
            <div className="mb-2 flex items-center gap-2">
              <TagIcon className="h-4 w-4 text-brand-700" />
              <p className="text-xs font-bold uppercase tracking-wide text-brand-700">Available offers</p>
            </div>
            <div className="flex flex-col gap-2">
              {availableCoupons
                .filter((offer) => offer.code !== cart.couponCode)
                .map((offer) => (
                  <div key={offer.code} className="flex items-center gap-3 rounded-2xl border border-brand-100 bg-brand-50/60 p-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-lg border border-dashed border-brand-500 bg-white px-2 py-1 font-mono text-xs font-black text-brand-800">
                          {offer.code}
                        </span>
                        <span className="text-[10px] font-bold uppercase tracking-wide text-ink-faint">
                          {offer.scope === 'STORE' ? 'Store offer' : 'AisleGo offer'}
                        </span>
                      </div>
                      <p className="mt-1.5 text-sm font-bold text-ink">
                        {offer.discountType === 'PERCENTAGE'
                          ? `${offer.percentOff}% off`
                          : `₹${offer.amountOff?.toFixed(0)} off`}
                        {offer.estimatedDiscount > 0 && ` · Save ₹${offer.estimatedDiscount.toFixed(0)} now`}
                      </p>
                      {offer.expiresAt && (
                        <p className="mt-0.5 text-xs text-ink-faint">Ends {new Date(offer.expiresAt).toLocaleDateString()}</p>
                      )}
                    </div>
                    <button
                      type="button"
                      className="shrink-0 rounded-xl bg-brand-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-60"
                      disabled={couponSubmitting}
                      onClick={() => applySuggestedCoupon(offer.code)}
                    >
                      Apply
                    </button>
                  </div>
                ))}
            </div>
          </div>
        )}
      </div>

      <div className="card flex flex-col gap-2">
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
      </div>

      <button type="button" onClick={() => navigate('/checkout')} className="btn-primary">
        Proceed to checkout
      </button>
    </div>
  )
}
