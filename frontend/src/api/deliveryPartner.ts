import { api } from './client'

export interface DeliveryPartnerProfile {
  id: number
  fullName: string
  phone: string
  available: boolean
  status: 'PENDING' | 'VERIFIED' | 'REJECTED'
  rejectionReason: string | null
}

export interface DeliveryOffer {
  orderId: number
  status: 'READY_FOR_PICKUP' | 'DELIVERY_PARTNER_ASSIGNED' | 'PICKED_UP' | 'OUT_FOR_DELIVERY' | 'DELIVERED'
  supermarketName: string
  branchName: string
  pickupAddress: string | null
  deliveryAddress: string
  fulfilmentType: 'IMMEDIATE' | 'SCHEDULED'
  scheduledFor: string | null
  itemCount: number
  orderTotal: number
  currency: string
}

export interface DeliveryLocation {
  available: boolean
  latitude: number | null
  longitude: number | null
  updatedAt: string | null
}

export interface DeliveryHistoryItem {
  orderId: number
  supermarketName: string
  branchName: string
  earning: number
  currency: string
  deliveredAt: string
}

export interface DeliveryEarnings {
  today: number
  total: number
  completedDeliveries: number
  currency: string
}

export const deliveryPartnerApi = {
  me: () => api.get<DeliveryPartnerProfile>('/delivery-partner/me'),
  updateAvailability: (available: boolean) =>
    api.patch<DeliveryPartnerProfile>('/delivery-partner/availability', { available }),
  listOffers: () => api.get<DeliveryOffer[]>('/delivery-partner/offers'),
  activeDelivery: () => api.get<DeliveryOffer | null>('/delivery-partner/active'),
  acceptOffer: (orderId: number) => api.post<DeliveryOffer>(`/delivery-partner/offers/${orderId}/accept`),
  verifyPickup: (orderId: number, code: string) => api.post<DeliveryOffer>(`/delivery-partner/deliveries/${orderId}/pickup/verify`, { code }),
  startDelivery: (orderId: number) => api.post<DeliveryOffer>(`/delivery-partner/deliveries/${orderId}/start`),
  verifyDelivery: (orderId: number, code: string) => api.post<DeliveryOffer>(`/delivery-partner/deliveries/${orderId}/delivery/verify`, { code }),
  updateLocation: (orderId: number, latitude: number, longitude: number) =>
    api.patch<DeliveryLocation>(`/delivery-partner/deliveries/${orderId}/location`, { latitude, longitude }),
  history: () => api.get<DeliveryHistoryItem[]>('/delivery-partner/history'),
  earnings: () => api.get<DeliveryEarnings>('/delivery-partner/earnings'),
}
