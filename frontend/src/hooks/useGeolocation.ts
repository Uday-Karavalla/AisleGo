import { useCallback, useState } from 'react'

export interface Coordinates {
  lat: number
  lng: number
}

export type GeolocationState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; coords: Coordinates }
  | { status: 'error'; message: string }

/**
 * Thin wrapper over the browser Geolocation API.
 * Callers must render a manual-entry fallback for the 'error' state
 * (permission denied, unsupported browser, or timeout).
 */
export function useGeolocation() {
  const [state, setState] = useState<GeolocationState>({ status: 'idle' })

  const detect = useCallback(() => {
    if (!('geolocation' in navigator)) {
      setState({ status: 'error', message: 'Location detection is not supported on this device.' })
      return
    }

    setState({ status: 'loading' })
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setState({
          status: 'success',
          coords: { lat: position.coords.latitude, lng: position.coords.longitude },
        })
      },
      (error) => {
        const message =
          error.code === error.PERMISSION_DENIED
            ? 'Location permission was denied. Enter your address instead.'
            : 'Could not detect your location. Enter your address instead.'
        setState({ status: 'error', message })
      },
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
    )
  }, [])

  return { state, detect }
}
