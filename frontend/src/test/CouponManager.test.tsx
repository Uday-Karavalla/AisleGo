import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { Coupon, CouponCrudApi } from '../api/coupons'
import { CouponManager } from '../components/CouponManager'

describe('CouponManager', () => {
  it('creates an upper-cased percentage coupon with the backend payload shape', async () => {
    const created: Coupon = {
      id: 7,
      code: 'SAVE15',
      supermarketId: 1,
      discountType: 'PERCENTAGE',
      percentOff: 15,
      amountOff: null,
      currency: null,
      expiresAt: null,
      active: true,
    }
    const api: CouponCrudApi = {
      list: vi.fn().mockResolvedValue([]),
      create: vi.fn().mockResolvedValue(created),
      update: vi.fn(),
      remove: vi.fn(),
    }
    const user = userEvent.setup()

    render(<CouponManager api={api} title="Store coupons" description="Store-only discounts" />)

    expect(await screen.findByText('No coupons yet.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /add coupon/i }))
    await user.type(screen.getByPlaceholderText(/coupon code/i), 'save15')
    await user.type(screen.getByPlaceholderText('Percent'), '15')
    await user.click(screen.getByRole('button', { name: /create coupon/i }))

    await waitFor(() => {
      expect(api.create).toHaveBeenCalledWith({
        code: 'SAVE15',
        discountType: 'PERCENTAGE',
        percentOff: 15,
        amountOff: null,
        currency: null,
        expiresAt: null,
      })
    })
    expect(await screen.findByText('15% off')).toBeInTheDocument()
  })
})
