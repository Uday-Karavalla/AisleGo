import { useEffect, useState } from 'react'
import { adminApi } from '../api/admin'
import type { PendingSupermarket } from '../api/admin'
import { Dialog } from '../components/Dialog'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

export default function AdminSupermarkets() {
  const [supermarkets, setSupermarkets] = useState<PendingSupermarket[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [rejectTarget, setRejectTarget] = useState<PendingSupermarket | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  function load() {
    setStatus('loading')
    adminApi
      .listPending()
      .then((list) => {
        setSupermarkets(list)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(() => {
    load()
  }, [])

  async function handleVerify(supermarket: PendingSupermarket) {
    setBusyId(supermarket.id)
    setMessage(null)
    try {
      await adminApi.verify(supermarket.id)
      setSupermarkets((prev) => prev.filter((item) => item.id !== supermarket.id))
      setMessage(`${supermarket.name} verified.`)
    } catch {
      setMessage(`Could not verify ${supermarket.name}. Please try again.`)
    } finally {
      setBusyId(null)
    }
  }

  function openReject(supermarket: PendingSupermarket) {
    setRejectTarget(supermarket)
    setRejectReason('')
  }

  async function handleReject() {
    if (!rejectTarget || !rejectReason.trim()) return
    const target = rejectTarget
    setBusyId(target.id)
    setMessage(null)
    try {
      await adminApi.reject(target.id, rejectReason.trim())
      setSupermarkets((prev) => prev.filter((item) => item.id !== target.id))
      setMessage(`${target.name} rejected.`)
      setRejectTarget(null)
    } catch {
      setMessage(`Could not reject ${target.name}. Please try again.`)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="flex flex-col gap-4 px-5 py-6">
      <h1 className="text-xl font-extrabold text-ink">Pending supermarkets</h1>

      {message && (
        <p role="status" className="text-sm text-brand-700">
          {message}
        </p>
      )}

      {status === 'loading' && (
        <div className="flex flex-col gap-3" aria-label="Loading pending supermarkets">
          {[0, 1].map((key) => (
            <div key={key} className="card h-24 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="Couldn't load pending supermarkets"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={load}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && supermarkets.length === 0 && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="No pending supermarkets"
          description="New registrations will show up here."
        />
      )}

      {status === 'success' &&
        supermarkets.map((supermarket) => (
          <div key={supermarket.id} className="card flex flex-col gap-2">
            <div>
              <h2 className="font-bold text-ink">{supermarket.name}</h2>
              {supermarket.description && <p className="text-sm text-ink-muted">{supermarket.description}</p>}
              <p className="text-sm text-ink-muted">
                {supermarket.ownerFullName ?? 'Unknown owner'} · {supermarket.ownerEmail ?? 'no email'}
              </p>
              {supermarket.phone && <p className="text-sm text-ink-muted">{supermarket.phone}</p>}
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-primary flex-1 py-2.5 text-sm"
                disabled={busyId === supermarket.id}
                onClick={() => handleVerify(supermarket)}
              >
                Verify
              </button>
              <button
                type="button"
                className="btn-secondary flex-1 py-2.5 text-sm"
                disabled={busyId === supermarket.id}
                onClick={() => openReject(supermarket)}
              >
                Reject
              </button>
            </div>
          </div>
        ))}

      <Dialog
        open={rejectTarget !== null}
        onClose={() => setRejectTarget(null)}
        title={`Reject ${rejectTarget?.name ?? ''}`}
        actions={
          <>
            <button
              type="button"
              className="btn-primary"
              disabled={!rejectReason.trim() || busyId === rejectTarget?.id}
              onClick={handleReject}
            >
              Confirm rejection
            </button>
            <button type="button" className="btn-ghost" onClick={() => setRejectTarget(null)}>
              Cancel
            </button>
          </>
        }
      >
        <label htmlFor="reject-reason" className="text-sm font-semibold text-ink">
          Reason for rejection
        </label>
        <textarea
          id="reject-reason"
          className="input-field mt-2"
          value={rejectReason}
          onChange={(event) => setRejectReason(event.target.value)}
          rows={3}
        />
      </Dialog>
    </div>
  )
}
