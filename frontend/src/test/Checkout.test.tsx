import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import Checkout from '../pages/Checkout'
import { ordersApi } from '../api/orders'
import type { CheckoutResponse, Order } from '../api/orders'
import { addressesApi } from '../api/addresses'
import type { Cart } from '../api/cart'

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

const mockClearCart = vi.fn()

const testCart: Cart = {
  id: 'local-cart',
  storeId: '5',
  storeName: 'Test Branch',
  items: [
    {
      id: 'item-1',
      productId: 'prod-1',
      storeId: '5',
      storeName: 'Test Branch',
      name: 'Apples',
      price: 50,
      unit: '1 kg',
      quantity: 2,
      allowSubstitution: true,
    },
  ],
  subtotal: 100,
  deliveryFee: 25,
  discount: 0,
  total: 125,
  couponCode: null,
}

vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    cart: testCart,
    isEmpty: false,
    clearCart: mockClearCart,
  }),
}))

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, email: 'jane@example.com', roles: ['CUSTOMER'], emailVerified: true },
  }),
}))

vi.mock('../api/orders', async () => {
  const actual = await vi.importActual<typeof import('../api/orders')>('../api/orders')
  return {
    ...actual,
    ordersApi: {
      checkout: vi.fn(),
      verifyPayment: vi.fn(),
      getById: vi.fn(),
      getStatus: vi.fn(),
    },
  }
})

vi.mock('../api/addresses', () => ({
  addressesApi: {
    list: vi.fn(),
    create: vi.fn(),
  },
}))

vi.mock('../api/mockOrders', () => ({
  createMockOrder: vi.fn(),
  loadMockOrder: vi.fn(),
  advanceMockOrder: vi.fn(),
}))

const testAddress = {
  id: '1',
  label: 'Home',
  line1: '1 Main St',
  city: 'Metropolis',
  state: 'State',
  postalCode: '123456',
  isDefault: true,
}

function baseOrder(overrides: Partial<Order> = {}): Order {
  return {
    id: 42,
    supermarketId: 1,
    branchId: 5,
    status: 'PLACED',
    fulfilmentType: 'IMMEDIATE',
    scheduledFor: null,
    subtotal: 100,
    deliveryFee: 25,
    totalAmount: 125,
    currency: 'INR',
    couponCode: null,
    discountAmount: 0,
    items: [],
    deliveryAddress: null,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
}

async function renderCheckoutReadyToSubmit() {
  const user = userEvent.setup()
  render(
    <MemoryRouter>
      <Checkout />
    </MemoryRouter>,
  )
  // Wait for the address list to load and auto-select the default address.
  await screen.findByText('Home')
  return user
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  vi.clearAllMocks()
  vi.mocked(addressesApi.list).mockResolvedValue([testAddress])
  delete (window as unknown as { Razorpay?: unknown }).Razorpay
})

describe('Checkout — mock payment provider (no client action)', () => {
  it('verifies payment immediately and navigates to order tracking', async () => {
    const checkoutResponse: CheckoutResponse = {
      order: baseOrder(),
      payment: {
        provider: 'MOCK',
        requiresClientAction: false,
        gatewayOrderId: null,
        providerKeyId: null,
        amountMinorUnits: 12500,
        currency: 'INR',
      },
    }
    vi.mocked(ordersApi.checkout).mockResolvedValue(checkoutResponse)
    vi.mocked(ordersApi.verifyPayment).mockResolvedValue(baseOrder({ status: 'PAYMENT_CONFIRMED' }))

    const user = await renderCheckoutReadyToSubmit()
    await user.click(screen.getByRole('button', { name: /pay & place order/i }))

    await waitFor(() => expect(ordersApi.verifyPayment).toHaveBeenCalledWith(42, {}, expect.any(String)))
    expect(ordersApi.checkout).toHaveBeenCalledWith(5, expect.any(String), 'IMMEDIATE', 1, undefined)
    expect(mockClearCart).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/orders/42')
  })

  it('removes the delivery fee for pickup and sends the fulfilment choice', async () => {
    const checkoutResponse: CheckoutResponse = {
      order: baseOrder({
        fulfilmentType: 'PICKUP',
        deliveryFee: 0,
        totalAmount: 100,
      }),
      payment: {
        provider: 'MOCK',
        requiresClientAction: false,
        gatewayOrderId: null,
        providerKeyId: null,
        amountMinorUnits: 10000,
        currency: 'INR',
      },
    }
    vi.mocked(ordersApi.checkout).mockResolvedValue(checkoutResponse)
    vi.mocked(ordersApi.verifyPayment).mockResolvedValue(
      baseOrder({ fulfilmentType: 'PICKUP', deliveryFee: 0, totalAmount: 100, status: 'PAYMENT_CONFIRMED' }),
    )

    const user = await renderCheckoutReadyToSubmit()
    await user.click(screen.getByRole('button', { name: 'Pickup' }))

    expect(screen.getByText('₹0')).toBeInTheDocument()
    expect(screen.getAllByText('₹100')).toHaveLength(2)

    await user.click(screen.getByRole('button', { name: /pay & place order/i }))
    await waitFor(() =>
      expect(ordersApi.checkout).toHaveBeenCalledWith(5, expect.any(String), 'PICKUP', undefined, undefined),
    )
  })
})

describe('Checkout — Razorpay payment provider (requires client action)', () => {
  it('opens the Razorpay widget with the gateway order id/key/amount and verifies on success', async () => {
    const checkoutResponse: CheckoutResponse = {
      order: baseOrder({ id: 77 }),
      payment: {
        provider: 'RAZORPAY',
        requiresClientAction: true,
        gatewayOrderId: 'order_rzp_test123',
        providerKeyId: 'rzp_test_key_id',
        amountMinorUnits: 12500,
        currency: 'INR',
      },
    }
    vi.mocked(ordersApi.checkout).mockResolvedValue(checkoutResponse)

    const openSpy = vi.fn()
    const RazorpayCtor = vi.fn().mockImplementation(() => ({ open: openSpy }))
    ;(window as unknown as { Razorpay: unknown }).Razorpay = RazorpayCtor

    const user = await renderCheckoutReadyToSubmit()
    await user.click(screen.getByRole('button', { name: /pay & place order/i }))

    await waitFor(() => expect(RazorpayCtor).toHaveBeenCalledTimes(1))
    expect(RazorpayCtor).toHaveBeenCalledWith(
      expect.objectContaining({
        key: 'rzp_test_key_id',
        order_id: 'order_rzp_test123',
        amount: 12500,
        currency: 'INR',
      }),
    )
    expect(openSpy).toHaveBeenCalledTimes(1)
    // verifyPayment must not be called until the widget's handler fires with a result.
    expect(ordersApi.verifyPayment).not.toHaveBeenCalled()
  })
})
