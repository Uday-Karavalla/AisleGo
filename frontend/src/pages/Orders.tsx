import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ordersApi } from '../api/orders'
import type { Order } from '../api/orders'
import { ORDER_STAGE_LABELS } from '../api/orders'
import { EmptyState } from '../components/EmptyState'
import { ClipboardIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [status, setStatus] = useState<Status>('loading')

  function load() {
    setStatus('loading')
    ordersApi
      .listMine()
      .then((list) => {
        setOrders(list)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <div className="flex flex-col gap-4 px-5 py-6">
      <h1 className="text-xl font-extrabold text-ink">Your orders</h1>

      {status === 'loading' && (
        <div className="flex flex-col gap-3" aria-label="Loading your orders">
          {[0, 1].map((key) => (
            <div key={key} className="card h-20 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<ClipboardIcon className="h-12 w-12" />}
          title="Couldn't load your orders"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={load}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && orders.length === 0 && (
        <EmptyState
          icon={<ClipboardIcon className="h-12 w-12" />}
          title="No orders yet"
          description="Once you place an order, it will show up here."
          action={
            <Link to="/stores" className="btn-primary">
              Browse stores
            </Link>
          }
        />
      )}

      {status === 'success' &&
        orders.map((order) => (
          <Link key={order.id} to={`/orders/${order.id}`} className="card flex flex-col gap-2 active:scale-[0.99]">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-ink">Order #{order.id}</h2>
              <span className="rounded-full bg-black/5 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                {ORDER_STAGE_LABELS[order.status]}
              </span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-ink-muted">{new Date(order.createdAt).toLocaleString()}</span>
              <span className="font-bold text-ink">
                {order.currency} {order.totalAmount.toFixed(2)}
              </span>
            </div>
          </Link>
        ))}
    </div>
  )
}
