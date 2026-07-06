import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'

export default function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setErrorMessage(null)
    try {
      const user = await login(email, password)
      if (user.roles.includes('ADMIN')) navigate('/admin')
      else if (user.roles.includes('SUPERMARKET_OWNER')) navigate('/my-store')
      else navigate('/')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setErrorMessage('Incorrect email or password.')
      } else {
        setErrorMessage(error instanceof Error ? error.message : 'Could not sign in. Please try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Sign in</h1>
        <p className="mt-1 text-sm text-ink-muted">Supermarket owners and AisleGo admins sign in here.</p>
      </div>

      <form onSubmit={handleSubmit} className="card flex flex-col gap-3">
        <label htmlFor="login-email" className="text-sm font-semibold text-ink">
          Email
        </label>
        <input
          id="login-email"
          type="email"
          className="input-field"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="email"
          required
        />

        <label htmlFor="login-password" className="text-sm font-semibold text-ink">
          Password
        </label>
        <input
          id="login-password"
          type="password"
          className="input-field"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          required
        />

        {errorMessage && (
          <p role="alert" className="text-sm text-danger-500">
            {errorMessage}
          </p>
        )}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="text-center text-sm text-ink-muted">
        Registering a supermarket?{' '}
        <Link to="/register-store" className="font-semibold text-brand-700">
          Register your store
        </Link>
      </p>
    </div>
  )
}
