import { api } from './client'

export type SupermarketStatus = 'PENDING' | 'VERIFIED' | 'REJECTED'

/** Matches the plan's `MySupermarketResponse` DTO for `GET /api/supermarkets/mine`. */
export interface MySupermarket {
  id: number
  name: string
  status: SupermarketStatus
  rejectionReason: string | null
}

export const supermarketOwnerApi = {
  mine: () => api.get<MySupermarket>('/supermarkets/mine'),
}
