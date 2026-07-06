import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AdminSupermarkets from '../pages/AdminSupermarkets'
import { adminApi } from '../api/admin'
import type { PendingSupermarket } from '../api/admin'

vi.mock('../api/admin', () => ({
  adminApi: {
    listPending: vi.fn(),
    verify: vi.fn(),
    reject: vi.fn(),
  },
}))

const pendingA: PendingSupermarket = {
  id: 1,
  name: 'Fresh Mart',
  description: 'Local grocery',
  phone: '9999999999',
  ownerEmail: 'owner@fresh.com',
  ownerFullName: 'Owner One',
}

const pendingB: PendingSupermarket = {
  id: 2,
  name: 'Green Grocer',
  description: null,
  phone: null,
  ownerEmail: 'owner2@green.com',
  ownerFullName: 'Owner Two',
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('AdminSupermarkets', () => {
  it('verifies a pending supermarket and removes it from the list', async () => {
    vi.mocked(adminApi.listPending).mockResolvedValue([pendingA, pendingB])
    vi.mocked(adminApi.verify).mockResolvedValue(undefined)

    const user = userEvent.setup()
    render(<AdminSupermarkets />)

    await screen.findByText('Fresh Mart')
    const verifyButtons = screen.getAllByRole('button', { name: /^verify$/i })
    await user.click(verifyButtons[0])

    await waitFor(() => expect(screen.queryByText('Fresh Mart')).not.toBeInTheDocument())
    expect(adminApi.verify).toHaveBeenCalledWith(1)
    expect(screen.getByText('Green Grocer')).toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent(/fresh mart verified/i)
  })

  it('rejects a pending supermarket with a reason and removes it from the list', async () => {
    vi.mocked(adminApi.listPending).mockResolvedValue([pendingA, pendingB])
    vi.mocked(adminApi.reject).mockResolvedValue(undefined)

    const user = userEvent.setup()
    render(<AdminSupermarkets />)

    await screen.findByText('Green Grocer')
    const rejectButtons = screen.getAllByRole('button', { name: /^reject$/i })
    await user.click(rejectButtons[1])

    await user.type(screen.getByLabelText(/reason for rejection/i), 'Incomplete documentation')
    await user.click(screen.getByRole('button', { name: /confirm rejection/i }))

    await waitFor(() => expect(screen.queryByText('Green Grocer')).not.toBeInTheDocument())
    expect(adminApi.reject).toHaveBeenCalledWith(2, 'Incomplete documentation')
    expect(screen.getByText('Fresh Mart')).toBeInTheDocument()
  })
})
