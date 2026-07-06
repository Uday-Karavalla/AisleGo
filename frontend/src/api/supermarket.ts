import { api } from './client'

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

export const supermarketOwnerApi = {
  mine: () => api.get<MySupermarket>('/supermarkets/mine'),

  listBranches: () => api.get<OwnerBranch[]>('/supermarkets/mine/branches'),
  createBranch: (branch: NewOwnerBranch) => api.post<OwnerBranch>('/supermarkets/mine/branches', branch),

  listProducts: () => api.get<OwnerProduct[]>('/supermarkets/mine/products'),
  createProduct: (product: NewOwnerProduct) => api.post<OwnerProduct>('/supermarkets/mine/products', product),
  updateProduct: (productId: number, product: UpdateOwnerProduct) =>
    api.patch<OwnerProduct>(`/supermarkets/mine/products/${productId}`, product),
  updateInventory: (productId: number, branchId: number, quantityOnHand: number) =>
    api.patch<OwnerProduct>(`/supermarkets/mine/products/${productId}/inventory`, { branchId, quantityOnHand }),
}
