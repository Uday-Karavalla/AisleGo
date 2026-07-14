import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { supermarketOwnerApi } from '../api/supermarket'
import type { OwnerOrder } from '../api/supermarket'
import { ORDER_STAGE_LABELS, type OrderStatus } from '../api/orders'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

const STATUS_FILTERS: Array<OrderStatus | 'ALL'> = [
  'ALL',
  'PLACED',
  'PAYMENT_CONFIRMED',
  'ACCEPTED_BY_STORE',
  'PICKING',
  'PACKING',
  'READY_FOR_PICKUP',
  'DELIVERY_PARTNER_ASSIGNED',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
]

/** The forward step a store can take from a given status — mirrors the backend's
 *  `OwnerOrderService.ALLOWED_TRANSITIONS`, which is the actual source of truth. */
const NEXT_STEP: Partial<Record<OrderStatus, { status: OrderStatus; label: string }>> = {
  PAYMENT_CONFIRMED: { status: 'ACCEPTED_BY_STORE', label: 'Accept order' },
  ACCEPTED_BY_STORE: { status: 'PICKING', label: 'Start picking' },
  PICKING: { status: 'PACKING', label: 'Mark packed' },
  PACKING: { status: 'READY_FOR_PICKUP', label: 'Mark ready for pickup' },
  OUT_FOR_DELIVERY: { status: 'DELIVERED', label: 'Mark delivered' },
}

const CANCELLABLE: OrderStatus[] = ['PAYMENT_CONFIRMED', 'ACCEPTED_BY_STORE', 'PICKING', 'PACKING', 'READY_FOR_PICKUP', 'DELIVERY_PARTNER_ASSIGNED', 'OUT_FOR_DELIVERY']

export default function MyStoreOrders() {
  const [orders, setOrders] = useState<OwnerOrder[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [filter, setFilter] = useState<OrderStatus | 'ALL'>('ALL')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  function load(statusFilter: OrderStatus | 'ALL') {
    setStatus('loading')
    supermarketOwnerApi
      .listOrders(statusFilter === 'ALL' ? undefined : statusFilter)
      .then((result) => {
        setOrders(result)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(() => {
    load(filter)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter])

  async function handleAdvance(order: OwnerOrder, nextStatus: OrderStatus) {
    setBusyId(order.id)
    setMessage(null)
    try {
      const updated = await supermarketOwnerApi.updateOrderStatus(order.id, nextStatus)
      setOrders((prev) => prev.map((item) => (item.id === order.id ? updated : item)))
      setMessage(`Order #${order.id} updated to ${ORDER_STAGE_LABELS[nextStatus]}.`)
    } catch {
      setMessage(`Could not update order #${order.id}. Please try again.`)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold text-ink">Orders</h1>
        <Link to="/my-store/catalogue" className="text-sm font-semibold text-brand-700">
          Manage catalogue
        </Link>
      </div>

      {message && (
        <p role="status" className="text-sm text-brand-700">
          {message}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map((option) => (
          <button
            key={option}
            type="button"
            className={
              option === filter
                ? 'rounded-full bg-brand-700 px-3 py-1 text-xs font-semibold text-white'
                : 'rounded-full bg-black/5 px-3 py-1 text-xs font-semibold text-ink-muted'
            }
            onClick={() => setFilter(option)}
          >
            {option === 'ALL' ? 'All' : ORDER_STAGE_LABELS[option]}
          </button>
        ))}
      </div>

      {status === 'loading' && (
        <div className="flex flex-col gap-3" aria-label="Loading orders">
          {[0, 1, 2].map((key) => (
            <div key={key} className="card h-24 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="Couldn't load orders"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={() => load(filter)}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && orders.length === 0 && (
        <EmptyState icon={<StoreIcon className="h-12 w-12" />} title="No orders found" description="Nothing matches this filter yet." />
      )}

      {status === 'success' &&
        orders.map((order) => {
          const next = order.status === 'READY_FOR_PICKUP'
            ? order.fulfilmentType === 'PICKUP'
              ? { status: 'DELIVERED' as const, label: 'Complete pickup' }
              : undefined
            : NEXT_STEP[order.status]
          const canCancel = CANCELLABLE.includes(order.status)
          return (
            <div key={order.id} className="card flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <h2 className="font-bold text-ink">Order #{order.id}</h2>
                <span className="rounded-full bg-black/5 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                  {ORDER_STAGE_LABELS[order.status]}
                </span>
              </div>
              <p className="text-sm text-ink-muted">
                {order.customerName} · {order.customerPhone ?? 'no phone'}
              </p>
              <p className="text-sm text-ink-muted">{order.branchName}</p>
              <p className="text-sm font-medium text-ink-muted">
                {order.fulfilmentType === 'PICKUP'
                  ? 'Store pickup'
                  : order.fulfilmentType === 'SCHEDULED' && order.scheduledFor
                    ? `Scheduled delivery: ${new Date(order.scheduledFor).toLocaleString()}`
                    : 'ASAP delivery'}
              </p>
              {order.deliveryAddress && (
                <p className="text-sm text-ink-muted">Deliver to: {order.deliveryAddress}</p>
              )}
              {order.status === 'READY_FOR_PICKUP' && order.fulfilmentType !== 'PICKUP' && (
                <p className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-700">Waiting for an available delivery partner.</p>
              )}
              <ul className="flex flex-col gap-0.5 text-sm text-ink-muted">
                {order.items.map((item) => (
                  <li key={item.productId}>
                    {item.quantity} × {item.productName}
                  </li>
                ))}
              </ul>
              <div className="flex items-center justify-between text-sm">
                <span className="text-ink-muted">{new Date(order.createdAt).toLocaleString()}</span>
                <div className="text-right">
                  {order.discountAmount > 0 && (
                    <p className="text-xs text-brand-700">{order.couponCode ?? 'Discount'}: -{order.currency} {order.discountAmount.toFixed(2)}</p>
                  )}
                  <p className="font-bold text-ink">{order.currency} {order.totalAmount.toFixed(2)}</p>
                </div>
              </div>
              {(next || canCancel) && (
                <div className="mt-1 flex gap-2">
                  {next && (
                    <button
                      type="button"
                      className="btn-primary flex-1 py-2.5 text-sm"
                      disabled={busyId === order.id}
                      onClick={() => handleAdvance(order, next.status)}
                    >
                      {next.label}
                    </button>
                  )}
                  {canCancel && (
                    <button
                      type="button"
                      className="btn-secondary flex-1 py-2.5 text-sm"
                      disabled={busyId === order.id}
                      onClick={() => handleAdvance(order, 'CANCELLED')}
                    >
                      Cancel
                    </button>
                  )}
                </div>
              )}
            </div>
          )
        })}
    </div>
  )
}
