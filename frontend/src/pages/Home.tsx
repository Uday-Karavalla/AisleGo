import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGeolocation } from '../hooks/useGeolocation'
import { useUserLocation } from '../context/LocationContext'
import { storesApi } from '../api/stores'
import { MapPinIcon, SearchIcon, StarIcon } from '../components/icons'

const TICKER_CATEGORIES = [
  { emoji: '🍌', label: 'Bananas' },
  { emoji: '🧃', label: 'Juices' },
  { emoji: '🥬', label: 'Veggies' },
  { emoji: '🫘', label: 'Pulses' },
  { emoji: '🍿', label: 'Snacks' },
  { emoji: '🧹', label: 'Household' },
  { emoji: '🍅', label: 'Tomatoes' },
  { emoji: '🍚', label: 'Rice' },
  { emoji: '🥛', label: 'Milk' },
  { emoji: '🍞', label: 'Bread' },
  { emoji: '🧴', label: 'Care' },
  { emoji: '🌶️', label: 'Spices' },
]

export default function Home() {
  const navigate = useNavigate()
  const { state, detect } = useGeolocation()
  const { location, setGpsLocation, setManualLocation } = useUserLocation()
  const [manualAddress, setManualAddress] = useState('')
  const [manualFormOpen, setManualFormOpen] = useState(false)
  const [isGeocoding, setIsGeocoding] = useState(false)
  const [manualError, setManualError] = useState<string | null>(null)

  useEffect(() => {
    if (state.status === 'success') {
      setGpsLocation(state.coords.lat, state.coords.lng)
      navigate('/stores')
    }
  }, [state, setGpsLocation, navigate])

  const showManualForm = manualFormOpen || state.status === 'error'

  async function handleManualSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = manualAddress.trim()
    if (!trimmed || isGeocoding) return
    setManualError(null)
    setIsGeocoding(true)
    try {
      const coords = await storesApi.geocode(trimmed)
      if (!coords) {
        setManualError("Couldn't find that address — try enabling location instead.")
        return
      }
      setManualLocation(trimmed, coords.lat, coords.lng)
      navigate('/stores')
    } catch {
      setManualError("Couldn't find that address — try enabling location instead.")
    } finally {
      setIsGeocoding(false)
    }
  }

  return (
    <div className="flex flex-col">
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-800 via-brand-700 to-brand-500 px-5 py-14 text-white md:px-10 lg:py-20">
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full bg-white/10 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-32 left-1/3 h-72 w-72 rounded-full bg-brand-300/20 blur-3xl"
          aria-hidden="true"
        />

        <div className="page-wide relative z-10 flex flex-col gap-10 lg:flex-row lg:items-center">
          <div className="flex flex-col gap-5 lg:w-1/2">
            <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5 text-xs font-semibold backdrop-blur">
              <StarIcon className="h-3.5 w-3.5 text-warning-500" />
              Fresh today · Delivered to your door
            </span>

            <h1 className="text-3xl font-extrabold leading-tight md:text-5xl">
              Everything your family needs, from stores you trust
            </h1>
            <p className="max-w-md text-sm text-white/80 md:text-base">
              Groceries, fresh produce, dairy and more — ordered from supermarkets near you and delivered to your
              door.
            </p>

            {location && (
              <button
                type="button"
                onClick={() => navigate('/stores')}
                className="flex items-center gap-3 rounded-2xl bg-white/10 px-4 py-3 text-left backdrop-blur transition active:scale-[0.99]"
              >
                <MapPinIcon className="h-5 w-5 shrink-0" />
                <span className="text-sm">
                  Continue with <strong>{location.label}</strong>
                </span>
              </button>
            )}

            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                type="button"
                onClick={detect}
                disabled={state.status === 'loading'}
                className="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3.5 text-base font-semibold text-brand-700 shadow-pop transition active:scale-[0.98] disabled:opacity-70"
              >
                <MapPinIcon className="h-5 w-5" />
                {state.status === 'loading' ? 'Detecting your location…' : 'Find stores near me'}
              </button>

              <button
                type="button"
                onClick={() => setManualFormOpen((prev) => !prev)}
                className="inline-flex items-center justify-center gap-2 rounded-2xl border-2 border-white/40 px-5 py-3.5 text-base font-semibold text-white transition active:scale-[0.98] active:bg-white/10"
              >
                <SearchIcon className="h-5 w-5" />
                Enter address manually
              </button>
            </div>

            {state.status === 'error' && (
              <p role="alert" className="text-sm text-warning-500">
                {state.message}
              </p>
            )}
          </div>

          <div className="hidden lg:block lg:w-[46%]">
            <div className="grid h-[380px] grid-cols-2 grid-rows-2 gap-4">
              <div className="row-span-2 flex flex-col justify-end rounded-3xl bg-gradient-to-br from-brand-200 to-emerald-400 p-4 shadow-pop">
                <span className="text-4xl">🥗</span>
                <p className="mt-2 text-sm font-bold text-brand-900">Fresh veggies daily</p>
              </div>
              <div className="flex flex-col justify-end rounded-3xl bg-gradient-to-br from-sky-100 to-sky-300 p-4 shadow-pop">
                <span className="text-3xl">🥛</span>
                <p className="mt-1 text-xs font-bold text-brand-900">Dairy &amp; more</p>
              </div>
              <div className="flex flex-col justify-end rounded-3xl bg-gradient-to-br from-orange-200 to-orange-400 p-4 shadow-pop">
                <span className="text-3xl">🍊</span>
                <p className="mt-1 text-xs font-bold text-brand-900">Fresh fruit</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {showManualForm && (
        <div className="page-shell justify-center px-5 py-6">
          <div className="page-narrow">
            <form onSubmit={handleManualSubmit} className="card flex flex-col gap-3">
              <label htmlFor="manual-address" className="text-sm font-semibold text-ink">
                Delivery address
              </label>
              <input
                id="manual-address"
                className="input-field"
                placeholder="House no, street, area, city"
                value={manualAddress}
                onChange={(event) => {
                  setManualAddress(event.target.value)
                  if (manualError) setManualError(null)
                }}
              />
              {manualError && (
                <p role="alert" className="text-sm text-danger-500">
                  {manualError}
                </p>
              )}
              <button type="submit" className="btn-primary" disabled={!manualAddress.trim() || isGeocoding}>
                {isGeocoding ? 'Finding address…' : 'Continue'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="overflow-hidden border-y border-black/5 bg-white py-3">
        <div className="flex w-max animate-marquee gap-6">
          {[...TICKER_CATEGORIES, ...TICKER_CATEGORIES].map((item, index) => (
            <span
              key={`${item.label}-${index}`}
              className="flex shrink-0 items-center gap-1.5 whitespace-nowrap text-sm font-semibold text-ink-muted"
            >
              <span aria-hidden="true">{item.emoji}</span>
              {item.label}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
