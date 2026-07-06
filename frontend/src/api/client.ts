// Small typed fetch wrapper shared by every resource module in src/api/.
// Base URL is configurable via VITE_API_BASE_URL so the frontend never hard-codes
// where the backend lives (and keeps working, via fixtures, before it exists).

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')

const AUTH_TOKEN_STORAGE_KEY = 'aislego.authToken'

export function getAuthToken(): string | null {
  try {
    return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

export function setAuthToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
    else localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    // localStorage unavailable (e.g. private mode) — auth simply won't persist.
  }
}

/** Normalized error shape every api/* module can rely on. */
export class ApiError extends Error {
  /** HTTP status code, or 0 for network-level failures (offline, DNS, CORS, backend not running yet). */
  status: number
  /** Machine-readable error code from the backend body, e.g. "CROSS_STORE_CONFLICT". */
  code?: string
  details?: unknown

  constructor(message: string, status: number, code?: string, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  get isNetworkError(): boolean {
    return this.status === 0
  }
}

export interface RequestOptions extends Omit<RequestInit, 'body' | 'headers'> {
  body?: unknown
  headers?: Record<string, string>
  /** Sent as the `Idempotency-Key` header — required for any request that must be retry-safe. */
  idempotencyKey?: string
}

interface ErrorPayload {
  message?: string
  error?: string
  code?: string
  [key: string]: unknown
}

async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, idempotencyKey, ...rest } = options
  const token = getAuthToken()

  const finalHeaders: Record<string, string> = {
    Accept: 'application/json',
    ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...headers,
  }
  if (token) finalHeaders.Authorization = `Bearer ${token}`
  if (idempotencyKey) finalHeaders['Idempotency-Key'] = idempotencyKey

  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...rest,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch (cause) {
    throw new ApiError(
      'Could not reach AisleGo servers. Check your connection and try again.',
      0,
      'NETWORK_ERROR',
      cause,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload: unknown = contentType.includes('application/json')
    ? await response.json().catch(() => undefined)
    : undefined

  if (!response.ok) {
    const errorPayload = (payload ?? {}) as ErrorPayload
    throw new ApiError(
      errorPayload.message ?? errorPayload.error ?? `Request failed with status ${response.status}`,
      response.status,
      errorPayload.code,
      payload,
    )
  }

  return payload as T
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'POST', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PATCH', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    apiFetch<T>(path, { ...options, method: 'PUT', body }),
  delete: <T>(path: string, options?: RequestOptions) => apiFetch<T>(path, { ...options, method: 'DELETE' }),
}
