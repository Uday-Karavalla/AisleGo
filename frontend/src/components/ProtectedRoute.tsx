import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface ProtectedRouteProps {
  /** Omit to only require "logged in, any role" - used by account-level pages like
   *  email verification that aren't specific to a customer/owner/admin. */
  requiredRole?: string
  children?: ReactNode
}

/** Gate for role-restricted routes. Supports both `<ProtectedRoute><X /></ProtectedRoute>`
 *  and nested-route (`<Outlet/>`) usage, matching whichever reads better at the call site. */
export function ProtectedRoute({ requiredRole, children }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return <div className="px-5 py-16 text-center text-sm text-ink-muted">Loading…</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && !user.roles.includes(requiredRole)) {
    return <Navigate to="/" replace />
  }

  return children ? <>{children}</> : <Outlet />
}
