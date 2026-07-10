import { api } from './client'
import type { OrderStatus } from './orders'

export type SupermarketStatus = 'PENDING' | 'VERIFIED' | 'REJECTED'

/** Matches the plan's `MySupermarketResponse` DTO for `GET /api/supermarkets/mine`. */
export interface MySupermarket {
  id: number
  name: string
  status: SupermarketStatus
  rejectionReason: string | null
}

export interface OwnerBranch {
  id: number
  name: string
  addressLine: string | null
  city: string | null
  latitude: number
  longitude: number
  openingTime: string | null
  closingTime: string | null
  active: boolean
}

export interface NewOwnerBranch {
  name: string
  addressLine: string
  city: string
  latitude: number
  longitude: number
  openingTime: string
  closingTime: string
}

export interface UpdateOwnerBranch extends NewOwnerBranch {
  active: boolean
}

export interface BranchStock {
  branchId: number
  branchName: string
  quantityOnHand: number
}

export interface OwnerProduct {
  id: number
  name: string
  description: string | null
  sku: string
  price: number
  currency: string
  categoryName: string | null
  imageUrl: string | null
  active: boolean
  branchStock: BranchStock[]
}

export interface NewOwnerProduct {
  name: string
  description?: string
  sku: string
  price: number
  currency: string
  categoryName?: string
  imageUrl?: string
  branchId: number
  initialStockQuantity: number
}

export interface UpdateOwnerProduct {
  name: string
  description?: string
  price: number
  currency: string
  categoryName?: string
  imageUrl?: string
  active: boolean
}

/** One item line of an owner-visible order — matches the backend's `OrderItemResponse`. */
export interface OwnerOrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

/** Matches the backend's `OwnerOrderResponse` exactly. */
export interface OwnerOrder {
  id: number
  customerName: string
  customerPhone: string | null
  branchId: number
  branchName: string
  status: OrderStatus
  totalAmount: number
  currency: string
  items: OwnerOrderItem[]
  deliveryAddress: string | null
  createdAt: string
}

export const supermarketOwnerApi = {
  mine: () => api.get<MySupermarket>('/supermarkets/mine'),

  listBranches: () => api.get<OwnerBranch[]>('/supermarkets/mine/branches'),
  createBranch: (branch: NewOwnerBranch) => api.post<OwnerBranch>('/supermarkets/mine/branches', branch),
  updateBranch: (branchId: number, branch: UpdateOwnerBranch) =>
    api.patch<OwnerBranch>(`/supermarkets/mine/branches/${branchId}`, branch),
  deleteBranch: (branchId: number) => api.delete<void>(`/supermarkets/mine/branches/${branchId}`),

  listProducts: () => api.get<OwnerProduct[]>('/supermarkets/mine/products'),
  createProduct: (product: NewOwnerProduct) => api.post<OwnerProduct>('/supermarkets/mine/products', product),
  updateProduct: (productId: number, product: UpdateOwnerProduct) =>
    api.patch<OwnerProduct>(`/supermarkets/mine/products/${productId}`, product),
  updateInventory: (productId: number, branchId: number, quantityOnHand: number) =>
    api.patch<OwnerProduct>(`/supermarkets/mine/products/${productId}/inventory`, { branchId, quantityOnHand }),
  deleteProduct: (productId: number) => api.delete<void>(`/supermarkets/mine/products/${productId}`),

  listOrders: (status?: OrderStatus) =>
    api.get<OwnerOrder[]>(`/supermarkets/mine/orders${status ? `?status=${status}` : ''}`),
  updateOrderStatus: (orderId: number, status: OrderStatus) =>
    api.patch<OwnerOrder>(`/supermarkets/mine/orders/${orderId}/status`, { status }),
}
