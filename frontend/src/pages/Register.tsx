import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'
import type { RegisterPayload } from '../api/auth'

const EMPTY_FORM: RegisterPayload = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
}

export default function Register() {
  const navigate = useNavigate()
  const { register } = useAuth()
  const [form, setForm] = useState<RegisterPayload>(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  function update<K extends keyof RegisterPayload>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setErrorMessage(null)
    try {
      await register(form)
      navigate('/stores')
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setErrorMessage('An account with that email already exists.')
      } else {
        setErrorMessage(error instanceof Error ? error.message : 'Could not create your account. Please try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <div className="page-shell justify-center px-5 py-8">
    <div className="page-narrow flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Create your account</h1>
        <p className="mt-1 text-sm text-ink-muted">Sign up to order from supermarkets near you.</p>
      </div>

      <form onSubmit={handleSubmit} className="card flex flex-col gap-3">
        <input
          className="input-field"
          placeholder="Full name"
          value={form.fullName}
          onChange={(event) => update('fullName', event.target.value)}
          autoComplete="name"
          required
        />
        <input
          className="input-field"
          type="email"
          placeholder="Email"
          value={form.email}
          onChange={(event) => update('email', event.target.value)}
          autoComplete="email"
          required
        />
        <input
          className="input-field"
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={(event) => update('password', event.target.value)}
          autoComplete="new-password"
          minLength={8}
          required
        />
        <input
          className="input-field"
          type="tel"
          placeholder="Phone number"
          value={form.phone}
          onChange={(event) => update('phone', event.target.value)}
          autoComplete="tel"
          required
        />

        {errorMessage && (
          <p role="alert" className="text-sm text-danger-500">
            {errorMessage}
          </p>
        )}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="text-center text-xs text-ink-faint">
        By creating an account, you agree to AisleGo's{' '}
        <Link to="/legal/terms" className="font-semibold text-brand-700">
          Terms of Service
        </Link>{' '}
        and{' '}
        <Link to="/legal/privacy" className="font-semibold text-brand-700">
          Privacy Policy
        </Link>
        .
      </p>

      <p className="text-center text-sm text-ink-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-brand-700">
          Sign in
        </Link>
      </p>
      <p className="text-center text-sm text-ink-muted">
        Registering a supermarket?{' '}
        <Link to="/register-store" className="font-semibold text-brand-700">
          Register your store
        </Link>
      </p>
    </div>
    </div>
  )
}
