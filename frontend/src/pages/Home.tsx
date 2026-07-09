import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGeolocation } from '../hooks/useGeolocation'
import { useUserLocation } from '../context/LocationContext'
import { storesApi } from '../api/stores'
import { MapPinIcon, SearchIcon } from '../components/icons'

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
    <div className="page-shell justify-center px-5 pb-10 pt-8">
      <div className="page-narrow flex flex-col gap-8">
      <div className="text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-brand-600 text-2xl font-black text-white shadow-card">
          A
        </div>
        <h1 className="mt-4 text-2xl font-extrabold leading-tight text-ink">
          Groceries from supermarkets you already trust
        </h1>
        <p className="mt-2 text-sm text-ink-muted">
          Share your location so we can show you supermarkets that deliver to your area.
        </p>
      </div>

      {location && (
        <button
          type="button"
          onClick={() => navigate('/stores')}
          className="card flex items-center gap-3 text-left transition active:scale-[0.99]"
        >
          <MapPinIcon className="h-5 w-5 shrink-0 text-brand-600" />
          <span className="text-sm text-ink-muted">
            Continue with <strong className="text-ink">{location.label}</strong>
          </span>
        </button>
      )}

      <div className="flex flex-col gap-3">
        <button
          type="button"
          onClick={detect}
          disabled={state.status === 'loading'}
          className="btn-primary"
        >
          <MapPinIcon className="h-5 w-5" />
          {state.status === 'loading' ? 'Detecting your location…' : 'Use my current location'}
        </button>

        {state.status === 'error' && (
          <p role="alert" className="text-center text-sm text-danger-500">
            {state.message}
          </p>
        )}

        <button type="button" onClick={() => setManualFormOpen((prev) => !prev)} className="btn-secondary">
          <SearchIcon className="h-5 w-5" />
          Enter address manually
        </button>

        {showManualForm && (
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
        )}
      </div>
      </div>
    </div>
  )
}
