import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useUserLocation } from '../context/LocationContext'
import { storesApi } from '../api/stores'
import type { Store } from '../api/stores'
import { StoreCard } from '../components/StoreCard'
import { EmptyState } from '../components/EmptyState'
import { MapPinIcon, SearchIcon, StoreIcon } from '../components/icons'
import { useAuth } from '../context/AuthContext'
import { useFavorites } from '../context/FavoritesContext'

type Status = 'loading' | 'success' | 'error'
type SortMode = 'recommended' | 'fastest' | 'closest' | 'rating'

export default function StoreDiscovery() {
  const { location } = useUserLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { supermarketIds: favoriteStoreIds, toggleStore } = useFavorites()
  const [stores, setStores] = useState<Store[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [retryToken, setRetryToken] = useState(0)
  const [query, setQuery] = useState('')
  const [openOnly, setOpenOnly] = useState(true)
  const [sortMode, setSortMode] = useState<SortMode>('recommended')
  const [favoritesOnly, setFavoritesOnly] = useState(false)

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

  const visibleStores = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const filtered = stores.filter((store) => {
      if (openOnly && !store.isOpen) return false
      if (favoritesOnly && (!store.supermarketId || !favoriteStoreIds.has(store.supermarketId))) return false
      if (!normalizedQuery) return true
      return `${store.name} ${store.address} ${(store.categories ?? []).join(' ')}`
        .toLowerCase()
        .includes(normalizedQuery)
    })
    return [...filtered].sort((a, b) => {
      if (sortMode === 'fastest') return a.etaMinutes - b.etaMinutes
      if (sortMode === 'closest') return a.distanceKm - b.distanceKm
      if (sortMode === 'rating') return (b.rating ?? 0) - (a.rating ?? 0)
      return Number(b.isOpen) - Number(a.isOpen) || a.etaMinutes - b.etaMinutes
    })
  }, [stores, query, openOnly, sortMode, favoritesOnly, favoriteStoreIds])

  if (!location) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="page-wide flex flex-col gap-5 px-5 py-6 md:py-9">
      <div className="rounded-3xl bg-gradient-to-r from-brand-800 to-brand-600 p-5 text-white shadow-card md:p-7">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-200">Shop nearby</p>
            <h1 className="mt-1 text-2xl font-extrabold md:text-3xl">Supermarkets around you</h1>
            <p className="mt-1 flex items-center gap-1.5 text-sm text-white/75">
              <MapPinIcon className="h-4 w-4" /> {location.label}
            </p>
          </div>
          <Link to="/" className="w-fit rounded-xl bg-white/10 px-3 py-2 text-xs font-bold backdrop-blur hover:bg-white/20">
            Change location
          </Link>
        </div>
      </div>

      {status === 'success' && stores.length > 0 && (
        <div className="card flex flex-col gap-3 md:flex-row md:items-center">
          <label className="relative min-w-0 flex-1">
            <span className="sr-only">Search supermarkets</span>
            <SearchIcon className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="input-field py-3 pl-10"
              placeholder="Search stores or categories"
            />
          </label>
          <div className="flex gap-2 overflow-x-auto pb-1 md:pb-0">
            {([
              ['recommended', 'Recommended'],
              ['fastest', 'Fastest'],
              ['closest', 'Closest'],
              ['rating', 'Top rated'],
            ] as const).map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setSortMode(value)}
                className={`shrink-0 rounded-xl px-3 py-2 text-xs font-bold transition ${
                  sortMode === value ? 'bg-brand-600 text-white' : 'bg-black/5 text-ink-muted hover:bg-black/10'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <label className="flex shrink-0 cursor-pointer items-center gap-2 text-xs font-bold text-ink-muted">
            <input
              type="checkbox"
              checked={openOnly}
              onChange={(event) => setOpenOnly(event.target.checked)}
              className="h-4 w-4 accent-brand-600"
            />
            Open now
          </label>
          {user?.roles.includes('CUSTOMER') && favoriteStoreIds.size > 0 && (
            <label className="flex shrink-0 cursor-pointer items-center gap-2 text-xs font-bold text-ink-muted">
              <input type="checkbox" checked={favoritesOnly} onChange={(event) => setFavoritesOnly(event.target.checked)} className="h-4 w-4 accent-brand-600" />
              Saved
            </label>
          )}
        </div>
      )}

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
        <>
          <div className="flex items-center justify-between">
            <p className="text-sm font-semibold text-ink-muted">
              {visibleStores.length} {visibleStores.length === 1 ? 'store' : 'stores'} found
            </p>
            <p className="hidden text-xs text-ink-faint sm:block">Delivery and pickup options shown at checkout</p>
          </div>
          {visibleStores.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {visibleStores.map((store) => (
                <StoreCard
                  key={store.id}
                  store={store}
                  onOpen={(selected) => navigate(`/stores/${selected.id}`)}
                  favorite={Boolean(store.supermarketId && favoriteStoreIds.has(store.supermarketId))}
                  onToggleFavorite={user?.roles.includes('CUSTOMER') && store.supermarketId
                    ? () => { void toggleStore(store.supermarketId!) }
                    : undefined}
                />
              ))}
            </div>
          ) : (
            <div className="card py-10 text-center">
              <p className="font-bold text-ink">No stores match those filters</p>
              <p className="mt-1 text-sm text-ink-muted">Try another search or include stores that are currently closed.</p>
              <button type="button" className="btn-ghost mt-3 text-brand-700" onClick={() => { setQuery(''); setOpenOnly(false) }}>
                Clear filters
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
