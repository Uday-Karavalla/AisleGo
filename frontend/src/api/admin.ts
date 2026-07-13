import { api } from './client'
import type { FulfilmentType, OrderStatus } from './orders'

export type SupermarketStatus = 'PENDING' | 'VERIFIED' | 'REJECTED'

/**
 * Shape of each element of `GET /api/admin/supermarkets`, matching the backend's
 * `PendingSupermarketResponse` DTO (id/name/description/phone/status/ownerEmail/ownerFullName,
 * nullable where the owner hasn't supplied a value or the store predates owner accounts).
 */
export interface PendingSupermarket {
  id: number
  name: string
  description: string | null
  phone: string | null
  status: SupermarketStatus
  ownerEmail: string | null
  ownerFullName: string | null
}

/** One row of `GET /api/admin/orders` — matches the backend's `AdminOrderResponse` exactly. */
export interface AdminOrder {
  id: number
  customerName: string
  customerEmail: string
  supermarketName: string
  branchName: string
  status: OrderStatus
  fulfilmentType: FulfilmentType
  scheduledFor: string | null
  subtotal: number
  deliveryFee: number
  totalAmount: number
  currency: string
  couponCode: string | null
  discountAmount: number
  deliveryAddress: string | null
  createdAt: string
}

/** Raw shape of `GET /api/admin/orders` - a Spring Data `Page<AdminOrderResponse>`. */
interface RawAdminOrderPage {
  content: AdminOrder[]
  number: number
  totalPages: number
  totalElements: number
}

export interface AdminOrderPage {
  orders: AdminOrder[]
  page: number
  totalPages: number
  totalCount: number
}

export const adminApi = {
  /** Omit `status` for the full store directory; pass it to filter to one review state. */
  listSupermarkets: (status?: SupermarketStatus) =>
    api.get<PendingSupermarket[]>(`/admin/supermarkets${status ? `?status=${status}` : ''}`),

  listPending: () => adminApi.listSupermarkets('PENDING'),

  verify: (id: number) => api.post<void>(`/admin/supermarkets/${id}/verify`),

  reject: (id: number, reason: string) => api.post<void>(`/admin/supermarkets/${id}/reject`, { reason }),

  async listOrders(params: { status?: OrderStatus; page?: number } = {}): Promise<AdminOrderPage> {
    const query = new URLSearchParams({
      page: String((params.page ?? 1) - 1),
      size: '20',
      ...(params.status ? { status: params.status } : {}),
    })
    const response = await api.get<RawAdminOrderPage>(`/admin/orders?${query.toString()}`)
    return {
      orders: response.content,
      page: response.number + 1,
      totalPages: response.totalPages,
      totalCount: response.totalElements,
    }
  },

  /** Manually marks a user's email verified - a stopgap for real customers who can't receive
   *  the actual verification email yet (Resend's free tier only delivers to the account owner's
   *  own address until a custom domain is verified). */
  verifyUserEmail: (email: string) => api.post<void>('/admin/users/verify-email', { email }),

  resetUserPassword: (email: string, newPassword: string) =>
    api.post<void>('/admin/users/reset-password', { email, newPassword }),
}
