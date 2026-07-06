import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'
import type { RegisterSupermarketOwnerPayload } from '../api/auth'

const EMPTY_FORM: RegisterSupermarketOwnerPayload = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  supermarketName: '',
  supermarketDescription: '',
  supermarketPhone: '',
}

export default function RegisterSupermarketOwner() {
  const navigate = useNavigate()
  const { registerSupermarketOwner } = useAuth()
  const [form, setForm] = useState<RegisterSupermarketOwnerPayload>(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  function update<K extends keyof RegisterSupermarketOwnerPayload>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setErrorMessage(null)
    try {
      await registerSupermarketOwner(form)
      navigate('/my-store')
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setErrorMessage('An account with that email already exists.')
      } else {
        setErrorMessage(error instanceof Error ? error.message : 'Could not register your store. Please try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Register your supermarket</h1>
        <p className="mt-1 text-sm text-ink-muted">
          Tell us about you and your store — an AisleGo admin will review it shortly.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <section className="card flex flex-col gap-3">
          <h2 className="text-sm font-bold text-ink">Your details</h2>
          <input
            className="input-field"
            placeholder="Full name"
            value={form.fullName}
            onChange={(event) => update('fullName', event.target.value)}
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
            required
          />
        </section>

        <section className="card flex flex-col gap-3">
          <h2 className="text-sm font-bold text-ink">Supermarket details</h2>
          <input
            className="input-field"
            placeholder="Supermarket name"
            value={form.supermarketName}
            onChange={(event) => update('supermarketName', event.target.value)}
            required
          />
          <textarea
            className="input-field"
            placeholder="Short description"
            value={form.supermarketDescription}
            onChange={(event) => update('supermarketDescription', event.target.value)}
            rows={3}
          />
          <input
            className="input-field"
            type="tel"
            placeholder="Supermarket phone"
            value={form.supermarketPhone}
            onChange={(event) => update('supermarketPhone', event.target.value)}
            required
          />
        </section>

        {errorMessage && (
          <p role="alert" className="text-sm text-danger-500">
            {errorMessage}
          </p>
        )}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Submitting…' : 'Submit for review'}
        </button>
      </form>
    </div>
  )
}
