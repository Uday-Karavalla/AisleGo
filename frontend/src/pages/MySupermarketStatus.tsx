import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { supermarketOwnerApi } from '../api/supermarket'
import type { MySupermarket } from '../api/supermarket'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

const STATUS_LABELS: Record<MySupermarket['status'], string> = {
  PENDING: 'Pending review',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
}

const STATUS_STYLES: Record<MySupermarket['status'], string> = {
  PENDING: 'bg-warning-50 text-warning-500',
  VERIFIED: 'bg-brand-50 text-brand-700',
  REJECTED: 'bg-danger-50 text-danger-500',
}

export default function MySupermarketStatus() {
  const [supermarket, setSupermarket] = useState<MySupermarket | null>(null)
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    setStatus('loading')
    supermarketOwnerApi
      .mine()
      .then((data) => {
        setSupermarket(data)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }, [])

  if (status === 'loading') {
    return <div className="px-5 py-16 text-center text-sm text-ink-muted">Loading your store…</div>
  }

  if (status === 'error' || !supermarket) {
    return (
      <EmptyState
        icon={<StoreIcon className="h-12 w-12" />}
        title="Couldn't load your supermarket"
        description="Check your connection and try again."
      />
    )
  }

  return (
    <div className="page-shell justify-center px-5 py-8">
    <div className="page-narrow flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-extrabold text-ink">{supermarket.name}</h1>
        <span
          className={`mt-2 inline-flex rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[supermarket.status]}`}
        >
          {STATUS_LABELS[supermarket.status]}
        </span>
      </div>

      {supermarket.status === 'PENDING' && (
        <p className="text-sm text-ink-muted">
          An AisleGo admin is reviewing your application. This usually takes a short while — check back soon.
        </p>
      )}

      {supermarket.status === 'VERIFIED' && (
        <p className="text-sm text-ink-muted">Your supermarket is live on AisleGo and visible to nearby customers.</p>
      )}

      {supermarket.status === 'REJECTED' && (
        <div className="card flex flex-col gap-2">
          <p className="text-sm font-semibold text-ink">Reason</p>
          <p className="text-sm text-ink-muted">{supermarket.rejectionReason ?? 'No reason provided.'}</p>
        </div>
      )}

      <Link to="/my-store/catalogue" className="btn-primary text-center">
        Manage branch &amp; products
      </Link>
      {supermarket.status === 'VERIFIED' && (
        <Link to="/my-store/orders" className="btn-secondary text-center">
          View orders
        </Link>
      )}
    </div>
    </div>
  )
}
