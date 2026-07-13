import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/auth'
import type { RegisterPayload, RegisterSupermarketOwnerPayload } from '../api/auth'
import { getAuthToken, setAuthToken } from '../api/client'

export interface AuthUser {
  id: number
  email: string
  roles: string[]
  emailVerified: boolean
}

interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<AuthUser>
  logout: () => void
  register: (payload: RegisterPayload) => Promise<AuthUser>
  registerSupermarketOwner: (payload: RegisterSupermarketOwnerPayload) => Promise<AuthUser>
  verifyEmail: (code: string) => Promise<void>
  resendVerification: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  // Starts true only when a token is present — otherwise there's nothing to rehydrate
  // and we can render the logged-out state immediately.
  const [isLoading, setIsLoading] = useState(() => getAuthToken() !== null)

  useEffect(() => {
    if (!getAuthToken()) return
    let cancelled = false
    authApi
      .me()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // Stale/expired token — clear it and stay logged out rather than looping.
        setAuthToken(null)
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // `AuthResponse` carries only tokens (no roles), so the simplest correct way to
  // populate `user` after login/registration is a follow-up call to `/auth/me`.
  const login = useCallback(async (email: string, password: string) => {
    const auth = await authApi.login(email, password)
    setAuthToken(auth.accessToken)
    const me = await authApi.me()
    setUser(me)
    return me
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    const auth = await authApi.register(payload)
    setAuthToken(auth.accessToken)
    const me = await authApi.me()
    setUser(me)
    return me
  }, [])

  const registerSupermarketOwner = useCallback(async (payload: RegisterSupermarketOwnerPayload) => {
    const response = await authApi.registerSupermarketOwner(payload)
    setAuthToken(response.auth.accessToken)
    const me = await authApi.me()
    setUser(me)
    return me
  }, [])

  const logout = useCallback(() => {
    setAuthToken(null)
    setUser(null)
    // Cart/location/addresses hold real personal data (home address, GPS coordinates) - on a
    // shared/public device, leaving them in localStorage after logout would let the next
    // person using the browser read the previous user's data straight out of storage.
    localStorage.removeItem('aislego.cart')
    localStorage.removeItem('aislego.location')
    localStorage.removeItem('aislego.addresses')
  }, [])

  const verifyEmail = useCallback(async (code: string) => {
    await authApi.verifyEmail(code)
    const me = await authApi.me()
    setUser(me)
  }, [])

  const resendVerification = useCallback(async () => {
    await authApi.resendVerification()
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      login,
      logout,
      register,
      registerSupermarketOwner,
      verifyEmail,
      resendVerification,
    }),
    [user, isLoading, login, logout, register, registerSupermarketOwner, verifyEmail, resendVerification],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}

/** Infrastructure providers such as the guest cart can operate before/without auth. */
export function useOptionalAuth(): AuthContextValue | undefined {
  return useContext(AuthContext)
}
