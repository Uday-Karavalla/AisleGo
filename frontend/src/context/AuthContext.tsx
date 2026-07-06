import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/auth'
import type { RegisterSupermarketOwnerPayload } from '../api/auth'
import { getAuthToken, setAuthToken } from '../api/client'

export interface AuthUser {
  id: number
  email: string
  roles: string[]
}

interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<AuthUser>
  logout: () => void
  registerSupermarketOwner: (payload: RegisterSupermarketOwnerPayload) => Promise<AuthUser>
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
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoading, login, logout, registerSupermarketOwner }),
    [user, isLoading, login, logout, registerSupermarketOwner],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
