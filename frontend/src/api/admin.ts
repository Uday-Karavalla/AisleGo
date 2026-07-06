import { api } from './client'

/**
 * Shape assumed for each element of `GET /api/admin/supermarkets?status=PENDING`,
 * matching the plan's `PendingSupermarketResponse` DTO description exactly
 * (id/name/description/phone/ownerEmail/ownerFullName, nullable where the owner
 * hasn't supplied a value).
 */
export interface PendingSupermarket {
  id: number
  name: string
  description: string | null
  phone: string | null
  ownerEmail: string | null
  ownerFullName: string | null
}

export const adminApi = {
  listPending: () => api.get<PendingSupermarket[]>('/admin/supermarkets?status=PENDING'),

  verify: (id: number) => api.post<void>(`/admin/supermarkets/${id}/verify`),

  reject: (id: number, reason: string) => api.post<void>(`/admin/supermarkets/${id}/reject`, { reason }),
}
