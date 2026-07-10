import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../api/admin'
import { ApiError } from '../api/client'

/** Admin-only account recovery tools: manually verify a user's email (a stopgap for real
 *  customers who can't receive the actual verification email yet - Resend's free tier only
 *  delivers to the account owner's own address until a custom domain is verified) and reset
 *  a user's password (lost-password recovery, no current password needed). */
export default function AdminUsers() {
  const [email, setEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showPasswordField, setShowPasswordField] = useState(false)
  const [busy, setBusy] = useState<'verify' | 'reset' | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)

  function report(text: string, error: boolean) {
    setMessage(text)
    setIsError(error)
  }

  async function handleVerifyEmail(event: FormEvent) {
    event.preventDefault()
    if (!email.trim()) return
    setBusy('verify')
    setMessage(null)
    try {
      await adminApi.verifyUserEmail(email.trim())
      report(`${email.trim()} is now verified.`, false)
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        report('No account found with that email.', true)
      } else {
        report(error instanceof Error ? error.message : 'Could not verify this email.', true)
      }
    } finally {
      setBusy(null)
    }
  }

  async function handleResetPassword(event: FormEvent) {
    event.preventDefault()
    if (!email.trim() || newPassword.length < 8) return
    setBusy('reset')
    setMessage(null)
    try {
      await adminApi.resetUserPassword(email.trim(), newPassword)
      report(`Password for ${email.trim()} has been reset.`, false)
      setNewPassword('')
      setShowPasswordField(false)
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        report('No account found with that email.', true)
      } else {
        report(error instanceof Error ? error.message : 'Could not reset this password.', true)
      }
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="page-narrow flex flex-col gap-6 px-5 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold text-ink">Manage users</h1>
        <Link to="/admin" className="text-sm font-semibold text-brand-700">
          Pending stores
        </Link>
      </div>

      {message && (
        <p role="alert" className={`text-sm ${isError ? 'text-danger-500' : 'text-brand-700'}`}>
          {message}
        </p>
      )}

      <section className="card flex flex-col gap-3">
        <label htmlFor="admin-user-email" className="text-sm font-semibold text-ink">
          User's email
        </label>
        <input
          id="admin-user-email"
          className="input-field"
          type="email"
          placeholder="customer@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <button type="button" className="btn-primary" disabled={!email.trim() || busy !== null} onClick={handleVerifyEmail}>
          {busy === 'verify' ? 'Verifying…' : 'Verify this email'}
        </button>
        <p className="text-xs text-ink-faint">
          Use this when a real customer/shopkeeper can't receive their verification code - it marks their account
          verified immediately, no code needed.
        </p>

        {!showPasswordField ? (
          <button type="button" className="btn-secondary" onClick={() => setShowPasswordField(true)}>
            Reset their password instead
          </button>
        ) : (
          <form onSubmit={handleResetPassword} className="flex flex-col gap-2">
            <input
              className="input-field"
              type="text"
              placeholder="New password (min 8 characters)"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
            />
            <div className="flex gap-2">
              <button
                type="submit"
                className="btn-primary flex-1"
                disabled={!email.trim() || newPassword.length < 8 || busy !== null}
              >
                {busy === 'reset' ? 'Resetting…' : 'Reset password'}
              </button>
              <button
                type="button"
                className="btn-secondary flex-1"
                onClick={() => {
                  setShowPasswordField(false)
                  setNewPassword('')
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  )
}
