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

function renderLogin() {
  return render(
    <MemoryRouter>
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
  it('logs an admin in and redirects to /admin', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-abc',
      refreshToken: 'refresh-abc',
      tokenType: 'Bearer',
      expiresInMillis: 3600000,
    })
    vi.mocked(authApi.me).mockResolvedValue({ id: 1, email: 'admin@aislego.com', roles: ['ADMIN'] })

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
    vi.mocked(authApi.me).mockResolvedValue({ id: 2, email: 'owner@store.com', roles: ['SUPERMARKET_OWNER'] })

    const user = userEvent.setup()
    renderLogin()
    await fillAndSubmit(user, 'owner@store.com', 'password123')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/my-store'))
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
