import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Login from '../pages/Login'
import { AuthProvider } from '../context/AuthContext'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/auth', () => ({
  authApi: {
    login: vi.fn(),
    registerSupermarketOwner: vi.fn(),
    me: vi.fn(),
  },
}))

function renderLogin(state?: { returnTo: string }) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state }]}>
      <AuthProvider>
        <Login />
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, email: string, password: string) {
  await user.type(screen.getByLabelText(/email/i), email)
  await user.type(screen.getByLabelText(/password/i), password)
  await user.click(screen.getByRole('button', { name: /sign in/i }))
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
})

describe('Login', () => {
  it('returns a customer to checkout after authentication', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-customer',
      refreshToken: 'refresh-customer',
      tokenType: 'Bearer',
      expiresInMillis: 3600000,
    })
    vi.mocked(authApi.me).mockResolvedValue({
      id: 3,
      email: 'shopper@example.com',
      roles: ['CUSTOMER'],
      emailVerified: true,
    })

    const user = userEvent.setup()
    renderLogin({ returnTo: '/checkout' })
    expect(screen.getByText(/your cart is waiting/i)).toBeInTheDocument()
    await fillAndSubmit(user, 'shopper@example.com', 'password123')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/checkout', { replace: true }))
  })

  it('logs an admin in and redirects to /admin', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-abc',
      refreshToken: 'refresh-abc',
      tokenType: 'Bearer',
      expiresInMillis: 3600000,
    })
    vi.mocked(authApi.me).mockResolvedValue({ id: 1, email: 'admin@aislego.com', roles: ['ADMIN'], emailVerified: true })

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'admin@aislego.com', 'password123')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/admin'))
    expect(authApi.login).toHaveBeenCalledWith('admin@aislego.com', 'password123')
  })

  it('logs a supermarket owner in and redirects to /my-store', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-abc',
      refreshToken: 'refresh-abc',
      tokenType: 'Bearer',
      expiresInMillis: 3600000,
    })
    vi.mocked(authApi.me).mockResolvedValue({
      id: 2,
      email: 'owner@store.com',
      roles: ['SUPERMARKET_OWNER'],
      emailVerified: true,
    })

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'owner@store.com', 'password123')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/my-store'))
  })

  it('logs a delivery partner in and redirects to /deliveries', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-delivery-partner',
      refreshToken: 'refresh-delivery-partner',
      tokenType: 'Bearer',
      expiresInMillis: 3600000,
    })
    vi.mocked(authApi.me).mockResolvedValue({
      id: 4,
      email: 'rider@example.com',
      roles: ['DELIVERY_PARTNER'],
      emailVerified: true,
    })

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'rider@example.com', 'password123')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/deliveries'))
  })

  it('shows an inline error and does not navigate on invalid credentials', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError('Unauthorized', 401))

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'wrong@example.com', 'badpassword')

    expect(await screen.findByRole('alert')).toHaveTextContent(/incorrect email or password/i)
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('shows a generic error for non-401 failures', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError('Could not reach AisleGo servers.', 0, 'NETWORK_ERROR'))

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'someone@example.com', 'password123')

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not reach aislego servers/i)
    expect(mockNavigate).not.toHaveBeenCalled()
  })
})
