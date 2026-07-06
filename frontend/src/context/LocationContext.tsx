import { createContext, useContext, useMemo } from 'react'
import type { ReactNode } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

export interface SelectedLocation {
  label: string
  lat: number
  lng: number
  source: 'gps' | 'manual'
}

interface LocationContextValue {
  location: SelectedLocation | null
  setGpsLocation: (lat: number, lng: number) => void
  /** Coordinates must already be resolved (e.g. via storesApi.geocode) before calling this. */
  setManualLocation: (label: string, lat: number, lng: number) => void
  clearLocation: () => void
}

// Named `useUserLocation` (not `useLocation`) to avoid colliding with react-router's hook of the same name.
const LocationContext = createContext<LocationContextValue | undefined>(undefined)

export function LocationProvider({ children }: { children: ReactNode }) {
  const [location, setLocation] = useLocalStorage<SelectedLocation | null>('aislego.location', null)

  const value = useMemo<LocationContextValue>(
    () => ({
      location,
      setGpsLocation: (lat, lng) =>
        setLocation({ label: `Near ${lat.toFixed(3)}, ${lng.toFixed(3)}`, lat, lng, source: 'gps' }),
      setManualLocation: (label, lat, lng) => setLocation({ label, lat, lng, source: 'manual' }),
      clearLocation: () => setLocation(null),
    }),
    [location, setLocation],
  )

  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>
}

export function useUserLocation(): LocationContextValue {
  const ctx = useContext(LocationContext)
  if (!ctx) throw new Error('useUserLocation must be used within a LocationProvider')
  return ctx
}
