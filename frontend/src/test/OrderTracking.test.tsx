import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ordersApi } from '../api/orders'
import type { Order } from '../api/orders'
import OrderTracking from '../pages/OrderTracking'

vi.mock('../api/orders', async () => {
  const actual = await vi.importActual<typeof import('../api/orders')>('../api/orders')
  return {
    ...actual,
    ordersApi: {
      getById: vi.fn(),
      getStatus: vi.fn(),
    },
  }
})

vi.mock('../api/mockOrders', () => ({
  loadMockOrder: vi.fn(),
  advanceMockOrder: vi.fn(),
}))

describe('OrderTracking coupon summary', () => {
  it('shows the snapshotted coupon and discount breakdown', async () => {
    const order: Order = {
      id: 42,
      supermarketId: 1,
      branchId: 2,
      status: 'PAYMENT_CONFIRMED',
      fulfilmentType: 'IMMEDIATE',
      scheduledFor: null,
      subtotal: 110,
      deliveryFee: 25,
      totalAmount: 125,
      currency: 'INR',
      couponCode: 'SAVE10',
      discountAmount: 10,
      items: [
        {
          productId: 5,
          productName: 'Rice',
          quantity: 1,
          unitPrice: 110,
          lineTotal: 110,
        },
      ],
      deliveryAddress: null,
      createdAt: '2026-07-13T06:30:00Z',
    }
    vi.mocked(ordersApi.getById).mockResolvedValue(order)

    render(
      <MemoryRouter initialEntries={['/orders/42']}>
        <Routes>
          <Route path="/orders/:orderId" element={<OrderTracking />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Coupon (SAVE10)')).toBeInTheDocument()
    expect(screen.getByText('-₹10')).toBeInTheDocument()
    expect(screen.getByText('₹25')).toBeInTheDocument()
    expect(screen.getByText('₹125')).toBeInTheDocument()
  })
})
