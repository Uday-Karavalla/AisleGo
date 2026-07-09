import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ApiError } from '../api/client'
import { CheckIcon } from '../components/icons'

export default function VerifyEmail() {
  const navigate = useNavigate()
  const { user, verifyEmail, resendVerification } = useAuth()
  const [code, setCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle')

  if (user?.emailVerified) {
    return (
      <div className="page-narrow flex flex-col items-center gap-3 px-5 py-16 text-center">
        <CheckIcon className="h-10 w-10 text-brand-600" />
        <h1 className="text-lg font-bold text-ink">Your email is verified</h1>
        <button type="button" className="btn-primary" onClick={() => navigate('/')}>
          Continue
        </button>
      </div>
    )
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!code.trim()) return
    setSubmitting(true)
    setErrorMessage(null)
    try {
      await verifyEmail(code.trim())
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setErrorMessage('That code is invalid or has expired. Try resending a new one.')
      } else {
        setErrorMessage(error instanceof Error ? error.message : 'Could not verify your email. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    setResendState('sending')
    setErrorMessage(null)
    try {
      await resendVerification()
      setResendState('sent')
    } catch {
      setErrorMessage('Could not resend the code. Please try again in a moment.')
      setResendState('idle')
    }
  }

  return (
    <div className="page-narrow flex flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Verify your email</h1>
        <p className="mt-1 text-sm text-ink-muted">
          We sent a 6-digit code to <strong className="text-ink">{user?.email}</strong>. Enter it below to verify
          your account.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="card flex flex-col gap-3">
        <label htmlFor="verification-code" className="text-sm font-semibold text-ink">
          Verification code
        </label>
        <input
          id="verification-code"
          className="input-field text-center text-lg tracking-[0.3em]"
          value={code}
          onChange={(event) => setCode(event.target.value)}
          inputMode="numeric"
          maxLength={6}
          placeholder="000000"
          autoComplete="one-time-code"
        />

        {errorMessage && (
          <p role="alert" className="text-sm text-danger-500">
            {errorMessage}
          </p>
        )}

        <button type="submit" className="btn-primary" disabled={submitting || !code.trim()}>
          {submitting ? 'Verifying…' : 'Verify email'}
        </button>
      </form>

      <div className="text-center text-sm text-ink-muted">
        Didn&apos;t get a code?{' '}
        <button
          type="button"
          className="font-semibold text-brand-700 disabled:text-ink-faint"
          onClick={handleResend}
          disabled={resendState === 'sending'}
        >
          {resendState === 'sent' ? 'Code resent — check your inbox' : 'Resend code'}
        </button>
      </div>
    </div>
  )
}
