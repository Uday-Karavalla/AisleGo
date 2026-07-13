import { api } from './client'

export interface ReferralSummary {
  referralCode: string
  invitedFriends: number
  rewardedFriends: number
  rewardCouponCodes: string[]
}

export const growthApi = {
  referralSummary: () => api.get<ReferralSummary>('/referrals/mine'),
}

export interface Favorites {
  productIds: number[]
  supermarketIds: number[]
}

export const favoritesApi = {
  list: () => api.get<Favorites>('/favorites'),
  addProduct: (id: string) => api.post<void>(`/favorites/products/${id}`),
  removeProduct: (id: string) => api.delete<void>(`/favorites/products/${id}`),
  addStore: (id: string) => api.post<void>(`/favorites/stores/${id}`),
  removeStore: (id: string) => api.delete<void>(`/favorites/stores/${id}`),
}

export interface UserNotification {
  id: number
  title: string
  message: string
  actionUrl: string | null
  read: boolean
  createdAt: string
}

export interface NotificationsResponse {
  unreadCount: number
  notifications: UserNotification[]
}

export const notificationsApi = {
  list: () => api.get<NotificationsResponse>('/notifications'),
  markRead: (id: number) => api.patch<void>(`/notifications/${id}/read`, {}),
}

export type GrowthEventName = 'page_view' | 'store_view' | 'search' | 'product_view' | 'add_to_cart' |
  'coupon_apply' | 'begin_checkout' | 'purchase' | 'share' | 'pwa_install'

function analyticsSessionId(): string {
  const key = 'aislego.analytics-session'
  let value = sessionStorage.getItem(key)
  if (!value) {
    value = crypto.randomUUID()
    sessionStorage.setItem(key, value)
  }
  return value
}

export function trackEvent(eventName: GrowthEventName, metadata: Record<string, unknown> = {}) {
  void api.post<void>('/growth/events', { eventName, sessionId: analyticsSessionId(), metadata }).catch(() => {})
}

export interface GrowthDashboard {
  periodDays: number
  visitors: number
  storeViews: number
  searches: number
  addToCarts: number
  checkouts: number
  purchases: number
  couponApplications: number
  checkoutConversionPercent: number
  dailyPurchases: Record<string, number>
}

export const analyticsApi = {
  dashboard: (days = 30) => api.get<GrowthDashboard>(`/growth/dashboard?days=${days}`),
}
