import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../api/admin'
import type { AdminOrder } from '../api/admin'
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
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
]

export default function AdminOrders() {
  const [orders, setOrders] = useState<AdminOrder[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [filter, setFilter] = useState<OrderStatus | 'ALL'>('ALL')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  function load(statusFilter: OrderStatus | 'ALL', pageNumber: number) {
    setStatus('loading')
    adminApi
      .listOrders({ status: statusFilter === 'ALL' ? undefined : statusFilter, page: pageNumber })
      .then((result) => {
        setOrders(result.orders)
        setTotalPages(result.totalPages)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(() => {
    load(filter, page)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, page])

  function handleFilterChange(next: OrderStatus | 'ALL') {
    setFilter(next)
    setPage(1)
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold text-ink">All orders</h1>
        <Link to="/admin" className="text-sm font-semibold text-brand-700">
          Pending supermarkets
        </Link>
      </div>

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
            onClick={() => handleFilterChange(option)}
          >
            {option === 'ALL' ? 'All' : ORDER_STAGE_LABELS[option]}
          </button>
        ))}
      </div>

      {status === 'loading' && (
        <div className="flex flex-col gap-3" aria-label="Loading orders">
          {[0, 1, 2].map((key) => (
            <div key={key} className="card h-20 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="Couldn't load orders"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={() => load(filter, page)}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && orders.length === 0 && (
        <EmptyState icon={<StoreIcon className="h-12 w-12" />} title="No orders found" description="Nothing matches this filter yet." />
      )}

      {status === 'success' &&
        orders.map((order) => (
          <div key={order.id} className="card flex flex-col gap-1">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-ink">Order #{order.id}</h2>
              <span className="rounded-full bg-black/5 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                {ORDER_STAGE_LABELS[order.status]}
              </span>
            </div>
            <p className="text-sm text-ink-muted">
              {order.customerName} · {order.customerEmail}
            </p>
            <p className="text-sm text-ink-muted">
              {order.supermarketName} — {order.branchName}
            </p>
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
            <div className="flex items-center justify-between text-sm">
              <span className="text-ink-muted">{new Date(order.createdAt).toLocaleString()}</span>
              <div className="text-right">
                {order.discountAmount > 0 && (
                  <p className="text-xs text-brand-700">{order.couponCode ?? 'Discount'}: -{order.currency} {order.discountAmount.toFixed(2)}</p>
                )}
                <p className="font-bold text-ink">{order.currency} {order.totalAmount.toFixed(2)}</p>
              </div>
            </div>
          </div>
        ))}

      {status === 'success' && totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <button
            type="button"
            className="btn-secondary px-4 py-2 text-sm disabled:opacity-40"
            disabled={page <= 1}
            onClick={() => setPage((prev) => Math.max(1, prev - 1))}
          >
            Previous
          </button>
          <span className="text-sm text-ink-muted">
            Page {page} of {totalPages}
          </span>
          <button
            type="button"
            className="btn-secondary px-4 py-2 text-sm disabled:opacity-40"
            disabled={page >= totalPages}
            onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
