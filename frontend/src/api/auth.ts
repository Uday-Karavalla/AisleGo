import { api } from './client'

/** Matches `AuthResponse.java` — shared by customer login and owner registration. */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInMillis: number
}

/** Matches the new `GET /api/auth/me` response, built from `AuthenticatedUser`. */
export interface MeResponse {
  id: number
  email: string
  roles: string[]
  emailVerified: boolean
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
  phone: string
  referralCode?: string
}

export interface RegisterSupermarketOwnerPayload {
  email: string
  password: string
  fullName: string
  phone: string
  supermarketName: string
  supermarketDescription: string
  supermarketPhone: string
}

export type RegisterDeliveryPartnerPayload = Pick<RegisterPayload, 'email' | 'password' | 'fullName' | 'phone'>

/**
 * Shape assumed for `POST /api/auth/register-supermarket-owner`'s response. The plan
 * leaves the wrapper's exact key names to the backend implementer's judgement — this
 * assumes it wraps the usual `AuthResponse` plus the newly-created supermarket's
 * id/status so the owner can be routed straight to a "pending review" screen. If the
 * backend lands with different key names, only this interface + the two call sites in
 * `AuthContext.tsx` need to change.
 */
export interface RegisterSupermarketOwnerResponse {
  auth: AuthResponse
  supermarketId: number
  supermarketStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
}

export const authApi = {
  login: (email: string, password: string) => api.post<AuthResponse>('/auth/login', { email, password }),

  register: (payload: RegisterPayload) => api.post<AuthResponse>('/auth/register', payload),

  registerSupermarketOwner: (payload: RegisterSupermarketOwnerPayload) =>
    api.post<RegisterSupermarketOwnerResponse>('/auth/register-supermarket-owner', payload),

  registerDeliveryPartner: (payload: RegisterDeliveryPartnerPayload) =>
    api.post<AuthResponse>('/auth/register-delivery-partner', payload),

  me: () => api.get<MeResponse>('/auth/me'),

  verifyEmail: (code: string) => api.post<void>('/auth/verify-email', { code }),

  resendVerification: () => api.post<void>('/auth/resend-verification'),
}
