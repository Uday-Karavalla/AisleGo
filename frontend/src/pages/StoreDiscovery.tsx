import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useUserLocation } from '../context/LocationContext'
import { storesApi } from '../api/stores'
import type { Store } from '../api/stores'
import { StoreCard } from '../components/StoreCard'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

export default function StoreDiscovery() {
  const { location } = useUserLocation()
  const navigate = useNavigate()
  const [stores, setStores] = useState<Store[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [retryToken, setRetryToken] = useState(0)

  useEffect(() => {
    if (!location) return
    let cancelled = false
    setStatus('loading')
    storesApi
      .nearby({ lat: location.lat, lng: location.lng })
      .then((response) => {
        if (cancelled) return
        setStores(response)
        setStatus('success')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [location, retryToken])

  if (!location) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Supermarkets near you</h1>
        <p className="text-sm text-ink-muted">Delivering to {location.label}</p>
      </div>

      {status === 'loading' && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-label="Loading nearby supermarkets">
          {[0, 1, 2].map((key) => (
            <div key={key} className="card h-24 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="Couldn't load nearby supermarkets"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={() => setRetryToken((n) => n + 1)}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && stores.length === 0 && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="No supermarkets nearby yet"
          description="We're expanding fast — please check back soon."
        />
      )}

      {status === 'success' && stores.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {stores.map((store) => (
            <StoreCard key={store.id} store={store} onOpen={(selected) => navigate(`/stores/${selected.id}`)} />
          ))}
        </div>
      )}
    </div>
  )
}
