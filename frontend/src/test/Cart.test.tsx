import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Cart from '../pages/Cart'
import { cartApi } from '../api/cart'

const { mockApplyCoupon } = vi.hoisted(() => ({ mockApplyCoupon: vi.fn() }))

vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    cart: {
      id: 'cart-1',
      storeId: '5',
      storeName: 'Neighbourhood Market',
      items: [
        {
          id: 'item-1',
          productId: 'product-1',
          storeId: '5',
          storeName: 'Neighbourhood Market',
          name: 'Rice',
          price: 100,
          unit: '1 kg',
          quantity: 1,
          allowSubstitution: true,
        },
      ],
      subtotal: 100,
      deliveryFee: 25,
      discount: 0,
      total: 125,
      couponCode: null,
    },
    isEmpty: false,
    updateQuantity: vi.fn(),
    setSubstitution: vi.fn(),
    removeItem: vi.fn(),
    applyCoupon: mockApplyCoupon,
  }),
}))

vi.mock('../api/cart', async () => {
  const actual = await vi.importActual<typeof import('../api/cart')>('../api/cart')
  return {
    ...actual,
    cartApi: {
      ...actual.cartApi,
      availableCoupons: vi.fn(),
    },
  }
})

describe('Cart available offers', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('aislego.authToken', 'test-token')
    vi.clearAllMocks()
  })

  it('shows an eligible coupon and applies it with one tap', async () => {
    vi.mocked(cartApi.availableCoupons).mockResolvedValue([
      {
        code: 'SAVE20',
        discountType: 'PERCENTAGE',
        percentOff: 20,
        amountOff: null,
        currency: null,
        expiresAt: null,
        scope: 'STORE',
        estimatedDiscount: 20,
      },
    ])
    mockApplyCoupon.mockResolvedValue(undefined)
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <Cart />
      </MemoryRouter>,
    )

    expect(await screen.findByText('SAVE20')).toBeInTheDocument()
    expect(screen.getByText(/save ₹20 now/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(mockApplyCoupon).toHaveBeenCalledWith('SAVE20')
  })
})
