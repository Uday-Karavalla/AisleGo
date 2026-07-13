import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Home from '../pages/Home'
import { LocationProvider } from '../context/LocationContext'
import { storesApi } from '../api/stores'

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/stores', () => ({
  storesApi: {
    geocode: vi.fn(),
  },
}))

function renderHome() {
  return render(
    <MemoryRouter>
      <LocationProvider>
        <Home />
      </LocationProvider>
    </MemoryRouter>,
  )
}

async function fillManualAddress(user: ReturnType<typeof userEvent.setup>, address: string) {
  await user.click(screen.getByRole('button', { name: /enter address manually/i }))
  await user.type(screen.getByLabelText(/delivery address/i), address)
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
})

describe('Home — manual address entry (geocode-then-navigate)', () => {
  it('explains the shopper value proposition and flexible fulfilment options', () => {
    renderHome()

    expect(screen.getByRole('heading', { name: /groceries you trust/i })).toBeInTheDocument()
    expect(screen.getByText('Coupons at checkout')).toBeInTheDocument()
    expect(screen.getByText('Free store pickup')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /more control over every grocery run/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /questions before your first order/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /share aislego/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /list your store/i })).toHaveAttribute('href', '/register-store')
  })

  it('resolves the typed address to real coordinates and navigates to /stores', async () => {
    vi.mocked(storesApi.geocode).mockResolvedValue({ lat: 12.34, lng: 56.78 })
    const user = userEvent.setup()
    renderHome()

    await fillManualAddress(user, '221B Baker Street')
    await user.click(screen.getByRole('button', { name: /continue/i }))

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/stores'))
    expect(storesApi.geocode).toHaveBeenCalledWith('221B Baker Street')

    const stored = JSON.parse(localStorage.getItem('aislego.location') ?? 'null')
    expect(stored).toMatchObject({ lat: 12.34, lng: 56.78, source: 'manual' })
  })

  it('shows an inline error and does not navigate or store fake coordinates when the address cannot be found', async () => {
    vi.mocked(storesApi.geocode).mockResolvedValue(null)
    const user = userEvent.setup()
    renderHome()

    await fillManualAddress(user, 'Nowhere at all')
    await user.click(screen.getByRole('button', { name: /continue/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/couldn't find that address/i)
    expect(mockNavigate).not.toHaveBeenCalled()
    expect(localStorage.getItem('aislego.location')).toBeNull()
  })
})
