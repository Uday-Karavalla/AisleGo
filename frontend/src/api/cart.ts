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

/** Error code the backend returns (409) when a product from a different store is added. */
export const CROSS_STORE_CONFLICT_CODE = 'CROSS_STORE_CONFLICT'

export interface CrossStoreConflictPayload {
  code: typeof CROSS_STORE_CONFLICT_CODE
  message: string
  currentStoreId: string
  currentStoreName: string
}

export const cartApi = {
  get: () => api.get<Cart>('/cart'),
  addItem: (body: AddCartItemRequest) => api.post<Cart>('/cart/items', body),
  updateItem: (itemId: string, body: UpdateCartItemRequest) => api.patch<Cart>(`/cart/items/${itemId}`, body),
  removeItem: (itemId: string) => api.delete<Cart>(`/cart/items/${itemId}`),
  clear: () => api.delete<Cart>('/cart'),
  applyCoupon: (code: string) => api.post<Cart>('/cart/coupon', { code }),
}
