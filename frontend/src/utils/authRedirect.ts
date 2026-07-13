export interface AuthRedirectState {
  returnTo?: unknown
}

/** Accept only internal app paths so auth state can never become an open redirect. */
export function safeReturnPath(state: unknown): string | null {
  const candidate = (state as AuthRedirectState | null)?.returnTo
  if (typeof candidate !== 'string' || !candidate.startsWith('/') || candidate.startsWith('//')) return null
  if (candidate === '/login' || candidate === '/register') return null
  return candidate
}
