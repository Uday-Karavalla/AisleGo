import { api } from './client'

export interface CartItem {
  /** Cart line-item id (server id once synced; client-generated uuid until then). */
  id: string
  productId: string
  storeId: string
  storeName: string
  name: string
  price: number
  unit: string
  imageUrl?: string
  quantity: number
  allowSubstitution: boolean
}

export interface Cart {
  id: string
  storeId: string | null
  storeName: string | null
  items: CartItem[]
  subtotal: number
  deliveryFee: number
  discount: number
  total: number
  couponCode?: string | null
}

export interface AddCartItemRequest {
  productId: string
  storeId: string
  quantity: number
  allowSubstitution?: boolean
}

export interface UpdateCartItemRequest {
  quantity?: number
  allowSubstitution?: boolean
}

/** Coupon fields returned by every server cart response. The rest of the cart is still
 * represented locally because the current backend response does not carry UI-only product
 * details such as unit, image URL, branch name, or substitution preference. */
export interface CartCouponState {
  subtotal: number
  deliveryFee: number
  couponCode: string | null
  discount: number
  total: number
}

export interface AvailableCoupon {
  code: string
  discountType: 'PERCENTAGE' | 'FLAT'
  percentOff: number | null
  amountOff: number | null
  currency: string | null
  expiresAt: string | null
  scope: 'STORE' | 'PLATFORM'
  estimatedDiscount: number
}

/** Error code the backend returns (409) when a product from a different store is added. */
export const CROSS_STORE_CONFLICT_CODE = 'CROSS_STORE_CONFLICT'

export interface CrossStoreConflictPayload {
  code: typeof CROSS_STORE_CONFLICT_CODE
  message: string
  currentStoreId: string
  currentStoreName: string
}

export const cartApi = {
  get: () => api.get<CartCouponState>('/cart'),
  addItem: (body: AddCartItemRequest) => api.post<CartCouponState>('/cart/items', body),
  updateItem: (itemId: string, body: UpdateCartItemRequest) =>
    api.patch<CartCouponState>(`/cart/items/${itemId}`, body),
  removeItem: (itemId: string) => api.delete<CartCouponState>(`/cart/items/${itemId}`),
  clear: () => api.delete<CartCouponState>('/cart'),
  applyCoupon: (code: string) => api.post<CartCouponState>('/cart/coupon', { code }),
  removeCoupon: () => api.delete<CartCouponState>('/cart/coupon'),
  availableCoupons: () => api.get<AvailableCoupon[]>('/cart/coupons'),
}
