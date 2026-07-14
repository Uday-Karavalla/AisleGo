import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { RegisterDeliveryPartnerPayload } from '../api/auth'
import { useAuth } from '../context/AuthContext'

const EMPTY_FORM: RegisterDeliveryPartnerPayload = { fullName: '', email: '', password: '', phone: '' }

export default function RegisterDeliveryPartner() {
  const navigate = useNavigate()
  const { registerDeliveryPartner } = useAuth()
  const [form, setForm] = useState(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function update<K extends keyof RegisterDeliveryPartnerPayload>(key: K, value: string) {
    setForm((previous) => ({ ...previous, [key]: value }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await registerDeliveryPartner(form)
      navigate('/deliveries', { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError && cause.status === 409
        ? 'An account with that email already exists.'
        : cause instanceof Error ? cause.message : 'Could not create your account.')
      setSubmitting(false)
    }
  }

  return (
    <div className="page-shell justify-center px-5 py-8">
      <div className="page-narrow flex flex-col gap-6">
        <div>
          <h1 className="text-xl font-extrabold text-ink">Deliver with AisleGo</h1>
          <p className="mt-1 text-sm text-ink-muted">Create your delivery-partner account. You will start offline.</p>
        </div>
        <form onSubmit={submit} className="card flex flex-col gap-3">
          <input className="input-field" placeholder="Full name" autoComplete="name" required value={form.fullName} onChange={(event) => update('fullName', event.target.value)} />
          <input className="input-field" type="email" placeholder="Email" autoComplete="email" required value={form.email} onChange={(event) => update('email', event.target.value)} />
          <input className="input-field" type="password" placeholder="Password" autoComplete="new-password" minLength={8} required value={form.password} onChange={(event) => update('password', event.target.value)} />
          <input className="input-field" type="tel" placeholder="Phone number" autoComplete="tel" required value={form.phone} onChange={(event) => update('phone', event.target.value)} />
          {error && <p role="alert" className="text-sm text-danger-500">{error}</p>}
          <button className="btn-primary" type="submit" disabled={submitting}>{submitting ? 'Creating account…' : 'Create partner account'}</button>
        </form>
        <p className="text-center text-sm text-ink-muted">Already registered? <Link to="/login" className="font-semibold text-brand-700">Sign in</Link></p>
      </div>
    </div>
  )
}
