import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../api/admin'
import type { AdminDeliveryPartner } from '../api/admin'
import { Dialog } from '../components/Dialog'

export default function AdminDeliveryPartners() {
  const [partners, setPartners] = useState<AdminDeliveryPartner[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [rejectTarget, setRejectTarget] = useState<AdminDeliveryPartner | null>(null)
  const [reason, setReason] = useState('')
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    adminApi.listPendingDeliveryPartners().then(setPartners).catch(() => setMessage('Could not load delivery partners.')).finally(() => setLoading(false))
  }, [])

  async function verify(partner: AdminDeliveryPartner) {
    setBusyId(partner.id)
    setMessage(null)
    try {
      await adminApi.verifyDeliveryPartner(partner.id)
      setPartners((current) => current.filter((item) => item.id !== partner.id))
      setMessage(`${partner.fullName} approved.`)
    } catch {
      setMessage(`Could not approve ${partner.fullName}.`)
    } finally {
      setBusyId(null)
    }
  }

  async function reject() {
    if (!rejectTarget || !reason.trim()) return
    const partner = rejectTarget
    setBusyId(partner.id)
    try {
      await adminApi.rejectDeliveryPartner(partner.id, reason.trim())
      setPartners((current) => current.filter((item) => item.id !== partner.id))
      setRejectTarget(null)
      setMessage(`${partner.fullName} rejected.`)
    } catch {
      setMessage(`Could not reject ${partner.fullName}.`)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold text-ink">Pending delivery partners</h1>
        <Link to="/admin" className="text-sm font-semibold text-brand-700">Pending stores</Link>
      </div>
      {message && <p role="status" className="text-sm text-brand-700">{message}</p>}
      {loading && <div className="card h-24 animate-pulse bg-black/5" />}
      {!loading && partners.length === 0 && <div className="card py-10 text-center text-sm text-ink-muted">No pending delivery partners.</div>}
      {partners.map((partner) => (
        <div key={partner.id} className="card flex flex-col gap-3">
          <div>
            <h2 className="font-bold text-ink">{partner.fullName}</h2>
            <p className="text-sm text-ink-muted">{partner.email} · {partner.phone}</p>
            <p className="text-xs text-ink-faint">Registered {new Date(partner.registeredAt).toLocaleString()}</p>
          </div>
          <div className="flex gap-2">
            <button className="btn-primary flex-1" disabled={busyId === partner.id} onClick={() => verify(partner)}>Approve</button>
            <button className="btn-secondary flex-1" disabled={busyId === partner.id} onClick={() => { setRejectTarget(partner); setReason('') }}>Reject</button>
          </div>
        </div>
      ))}
      <Dialog open={rejectTarget !== null} onClose={() => setRejectTarget(null)} title={`Reject ${rejectTarget?.fullName ?? ''}`}
        actions={<><button className="btn-primary" disabled={!reason.trim()} onClick={reject}>Confirm rejection</button><button className="btn-ghost" onClick={() => setRejectTarget(null)}>Cancel</button></>}>
        <label htmlFor="partner-rejection" className="text-sm font-semibold text-ink">Reason for rejection</label>
        <textarea id="partner-rejection" className="input-field mt-2" rows={3} value={reason} onChange={(event) => setReason(event.target.value)} />
      </Dialog>
    </div>
  )
}
