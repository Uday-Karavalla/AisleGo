import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ordersApi } from '../api/orders'
import type { Order } from '../api/orders'
import { ApiError } from '../api/client'
import { loadMockOrder, advanceMockOrder } from '../api/mockOrders'
import { OrderStatusStepper } from '../components/OrderStatusStepper'
import { ClockIcon } from '../components/icons'

const POLL_INTERVAL_MS = 4000

type Status = 'loading' | 'success' | 'error'

export default function OrderTracking() {
  const { orderId: orderIdParam } = useParams<{ orderId: string }>()
  const orderId = orderIdParam !== undefined ? Number(orderIdParam) : undefined
  const [order, setOrder] = useState<Order | null>(null)
  const [status, setStatus] = useState<Status>('loading')

  // Initial full-order fetch (items, totals, status).
  useEffect(() => {
    if (orderId === undefined || Number.isNaN(orderId)) {
      setStatus('error')
      return
    }
    let cancelled = false

    ordersApi
      .getById(orderId)
      .then((fetched) => {
        if (cancelled) return
        setOrder(fetched)
        setStatus('success')
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (error instanceof ApiError && error.isNetworkError) {
          const mock = loadMockOrder(orderId)
          if (mock) {
            setOrder(mock)
            setStatus('success')
            return
          }
        }
        setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [orderId])

  // Lightweight status-only polling once the order has loaded.
  useEffect(() => {
    if (orderId === undefined || Number.isNaN(orderId) || status !== 'success') return

    const interval = setInterval(() => {
      ordersApi
        .getStatus(orderId)
        .then(({ status: nextStatus }) => {
          setOrder((prev) => (prev ? { ...prev, status: nextStatus } : prev))
        })
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.isNetworkError) {
            const advanced = advanceMockOrder(orderId)
            if (advanced) setOrder(advanced)
          }
        })
    }, POLL_INTERVAL_MS)

    return () => clearInterval(interval)
  }, [orderId, status])

  if (status === 'loading') {
    return <div className="px-5 py-10 text-center text-sm text-ink-muted">Loading your order…</div>
  }

  if (status === 'error' || !order) {
    return <div className="px-5 py-10 text-center text-sm text-danger-500">We couldn&apos;t find this order.</div>
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">Order #{order.id}</p>
        <h1 className="text-xl font-extrabold text-ink">Track your order</h1>
        <p className="mt-1 flex items-center gap-1 text-sm text-ink-muted">
          <ClockIcon className="h-4 w-4" />
          Placed {new Date(order.createdAt).toLocaleString()}
        </p>
      </div>

      <div className="card">
        <OrderStatusStepper currentStage={order.status} />
      </div>

      <div className="card">
        <h2 className="mb-3 text-sm font-bold text-ink">Order summary</h2>
        <ul className="flex flex-col gap-2 text-sm">
          {order.items.map((item) => (
            <li key={item.productId} className="flex justify-between text-ink-muted">
              <span>
                {item.quantity} × {item.productName}
              </span>
              <span>₹{item.lineTotal.toFixed(0)}</span>
            </li>
          ))}
        </ul>
        <div className="mt-3 flex justify-between border-t border-black/5 pt-3 text-sm font-bold text-ink">
          <span>Total</span>
          <span>₹{order.totalAmount.toFixed(0)}</span>
        </div>
      </div>
    </div>
  )
}
