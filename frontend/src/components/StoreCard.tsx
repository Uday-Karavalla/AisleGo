import type { Store } from '../api/stores'
import { StarIcon, ClockIcon, MapPinIcon, StoreIcon, ChevronRightIcon, HeartIcon } from './icons'

interface StoreCardProps {
  store: Store
  onOpen: (store: Store) => void
  favorite?: boolean
  onToggleFavorite?: () => void
}

export function StoreCard({ store, onOpen, favorite, onToggleFavorite }: StoreCardProps) {
  return (
    <div className="card group relative flex w-full items-center gap-4 overflow-hidden border border-transparent text-left transition hover:-translate-y-0.5 hover:border-brand-100 hover:shadow-pop">
      <button type="button" onClick={() => onOpen(store)} disabled={!store.isOpen} className="absolute inset-0 z-0 disabled:cursor-not-allowed" aria-label={`Open ${store.name}`} />
      <span className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-400 via-brand-600 to-brand-800 opacity-0 transition group-hover:opacity-100" />
      <div className="pointer-events-none flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
        {store.logoUrl ? (
          <img src={store.logoUrl} alt={`${store.name} logo`} className="h-full w-full rounded-2xl object-cover" />
        ) : (
          <StoreIcon className="h-8 w-8" />
        )}
      </div>

      <div className="pointer-events-none min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <h3 className="truncate text-base font-semibold text-ink">{store.name}</h3>
          {!store.isOpen && (
            <span className="shrink-0 rounded-full bg-danger-50 px-2 py-0.5 text-xs font-semibold text-danger-500">
              Closed
            </span>
          )}
        </div>

        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-ink-muted">
          {typeof store.rating === 'number' && (
            <span className="inline-flex items-center gap-1">
              <StarIcon className="h-3.5 w-3.5 text-warning-500" />
              {store.rating.toFixed(1)}
              {typeof store.ratingCount === 'number' && (
                <span className="text-ink-faint">({store.ratingCount})</span>
              )}
            </span>
          )}
          <span className="inline-flex items-center gap-1">
            <MapPinIcon className="h-3.5 w-3.5" />
            {store.distanceKm.toFixed(1)} km
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg bg-brand-50 px-2 py-1 font-semibold text-brand-800">
            <ClockIcon className="h-3.5 w-3.5" />
            {store.isOpen ? `${store.etaMinutes} min` : 'Opens later'}
          </span>
        </div>

        {store.categories && store.categories.length > 0 && (
          <p className="mt-1 truncate text-xs text-ink-faint">{store.categories.join(' · ')}</p>
        )}
      </div>

      {store.isOpen && (
        <span className="pointer-events-none flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700 transition group-hover:bg-brand-600 group-hover:text-white">
          <ChevronRightIcon className="h-5 w-5" />
        </span>
      )}
      {onToggleFavorite && (
        <button type="button" onClick={onToggleFavorite} className="absolute right-2 top-2 z-10 rounded-full bg-white p-1.5 text-danger-500 shadow-card" aria-label={favorite ? `Remove ${store.name} from favourites` : `Save ${store.name} to favourites`}>
          <HeartIcon className="h-4 w-4" filled={favorite} />
        </button>
      )}
    </div>
  )
}
