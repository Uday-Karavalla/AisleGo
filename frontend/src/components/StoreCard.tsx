import type { Store } from '../api/stores'
import { StarIcon, ClockIcon, MapPinIcon, StoreIcon, ChevronRightIcon } from './icons'

interface StoreCardProps {
  store: Store
  onOpen: (store: Store) => void
}

export function StoreCard({ store, onOpen }: StoreCardProps) {
  return (
    <button
      type="button"
      onClick={() => onOpen(store)}
      disabled={!store.isOpen}
      className="card flex w-full items-center gap-4 text-left transition active:scale-[0.99] disabled:opacity-60"
    >
      <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
        {store.logoUrl ? (
          <img src={store.logoUrl} alt={`${store.name} logo`} className="h-full w-full rounded-2xl object-cover" />
        ) : (
          <StoreIcon className="h-8 w-8" />
        )}
      </div>

      <div className="min-w-0 flex-1">
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
          <span className="inline-flex items-center gap-1">
            <ClockIcon className="h-3.5 w-3.5" />
            {store.isOpen ? `${store.etaMinutes} min` : 'Opens later'}
          </span>
        </div>

        {store.categories && store.categories.length > 0 && (
          <p className="mt-1 truncate text-xs text-ink-faint">{store.categories.join(' · ')}</p>
        )}
      </div>

      {store.isOpen && <ChevronRightIcon className="h-5 w-5 shrink-0 text-ink-faint" />}
    </button>
  )
}
