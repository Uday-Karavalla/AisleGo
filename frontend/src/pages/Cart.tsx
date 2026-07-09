import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useCart } from '../context/CartContext'
import { QuantityStepper } from '../components/QuantityStepper'
import { EmptyState } from '../components/EmptyState'
import { CartIcon, XIcon } from '../components/icons'

export default function Cart() {
  const navigate = useNavigate()
  const { cart, isEmpty, updateQuantity, setSubstitution, removeItem, applyCoupon } = useCart()
  const [couponInput, setCouponInput] = useState(cart.couponCode ?? '')

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
        <div className="flex gap-2">
          <input
            id="coupon"
            className="input-field flex-1"
            placeholder="Enter coupon code"
            value={couponInput}
            onChange={(event) => setCouponInput(event.target.value)}
          />
          <button type="button" className="btn-secondary px-4" onClick={() => applyCoupon(couponInput)}>
            Apply
          </button>
        </div>
        {cart.couponCode && <p className="text-xs text-brand-700">&ldquo;{cart.couponCode}&rdquo; applied.</p>}
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
