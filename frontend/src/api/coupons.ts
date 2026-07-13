import { api } from './client'

export type DiscountType = 'PERCENTAGE' | 'FLAT'

export interface Coupon {
  id: number
  code: string
  supermarketId: number | null
  discountType: DiscountType
  percentOff: number | null
  amountOff: number | null
  currency: string | null
  expiresAt: string | null
  active: boolean
  firstOrderOnly?: boolean
  maxRedemptions?: number | null
  perUserLimit?: number | null
}

export interface CreateCouponPayload {
  code: string
  discountType: DiscountType
  percentOff: number | null
  amountOff: number | null
  currency: string | null
  expiresAt: string | null
  firstOrderOnly?: boolean
  maxRedemptions?: number | null
  perUserLimit?: number | null
}

export interface UpdateCouponPayload extends Omit<CreateCouponPayload, 'code'> {
  active: boolean
}

export interface CouponCrudApi {
  list: () => Promise<Coupon[]>
  create: (payload: CreateCouponPayload) => Promise<Coupon>
  update: (couponId: number, payload: UpdateCouponPayload) => Promise<Coupon>
  remove: (couponId: number) => Promise<void>
}

export const ownerCouponApi: CouponCrudApi = {
  list: () => api.get<Coupon[]>('/supermarkets/mine/coupons'),
  create: (payload) => api.post<Coupon>('/supermarkets/mine/coupons', payload),
  update: (couponId, payload) => api.patch<Coupon>(`/supermarkets/mine/coupons/${couponId}`, payload),
  remove: (couponId) => api.delete<void>(`/supermarkets/mine/coupons/${couponId}`),
}

export const adminCouponApi: CouponCrudApi = {
  list: () => api.get<Coupon[]>('/admin/coupons'),
  create: (payload) => api.post<Coupon>('/admin/coupons', payload),
  update: (couponId, payload) => api.patch<Coupon>(`/admin/coupons/${couponId}`, payload),
  remove: (couponId) => api.delete<void>(`/admin/coupons/${couponId}`),
}
