import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ordersApi } from '../api/orders'
import type { Order } from '../api/orders'
import { ORDER_STAGE_LABELS } from '../api/orders'
import { EmptyState } from '../components/EmptyState'
import { ClipboardIcon } from '../components/icons'
import { useCart } from '../context/CartContext'

type Status = 'loading' | 'success' | 'error'

export default function Orders() {
  const navigate = useNavigate()
  const { replaceWithOrder } = useCart()
  const [orders, setOrders] = useState<Order[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [reorderingId, setReorderingId] = useState<number | null>(null)

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

  async function reorder(orderId: number) {
    setReorderingId(orderId)
    try {
      const order = orders.find((candidate) => candidate.id === orderId)
      if (!order) return
      const serverCart = await ordersApi.reorder(orderId)
      replaceWithOrder(order, serverCart)
      navigate('/cart')
    } finally {
      setReorderingId(null)
    }
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <h1 className="text-xl font-extrabold text-ink">Your orders</h1>

      {status === 'loading' && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-label="Loading your orders">
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

      {status === 'success' && orders.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {orders.map((order) => (
            <div key={order.id} className="card flex flex-col gap-2">
              <Link to={`/orders/${order.id}`} className="flex flex-col gap-2 active:scale-[0.99]">
              <div className="flex items-center justify-between">
                <h2 className="font-bold text-ink">Order #{order.id}</h2>
                <span className="rounded-full bg-black/5 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                  {ORDER_STAGE_LABELS[order.status]}
                </span>
              </div>
              <p className="text-sm text-ink-muted">
                {order.fulfilmentType === 'PICKUP'
                  ? 'Store pickup'
                  : order.fulfilmentType === 'SCHEDULED' && order.scheduledFor
                    ? `Scheduled ${new Date(order.scheduledFor).toLocaleString()}`
                    : 'ASAP delivery'}
              </p>
              <div className="flex items-center justify-between text-sm">
                <span className="text-ink-muted">{new Date(order.createdAt).toLocaleString()}</span>
                <div className="text-right">
                  {order.discountAmount > 0 && (
                    <p className="text-xs text-brand-700">Saved {order.currency} {order.discountAmount.toFixed(2)}</p>
                  )}
                  <p className="font-bold text-ink">{order.currency} {order.totalAmount.toFixed(2)}</p>
                </div>
              </div>
              </Link>
              <button type="button" className="btn-secondary py-2 text-xs" onClick={() => reorder(order.id)} disabled={reorderingId === order.id}>
                {reorderingId === order.id ? 'Adding items…' : 'Order these items again'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
