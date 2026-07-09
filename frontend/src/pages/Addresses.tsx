import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { addressesApi } from '../api/addresses'
import type { Address, NewAddress } from '../api/addresses'
import { Dialog } from '../components/Dialog'
import { EmptyState } from '../components/EmptyState'
import { MapPinIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

const EMPTY_DRAFT: NewAddress = {
  label: 'Home',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postalCode: '',
  isDefault: false,
}

export default function Addresses() {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [message, setMessage] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [draft, setDraft] = useState<NewAddress>(EMPTY_DRAFT)
  const [submitting, setSubmitting] = useState(false)

  const [deleteTarget, setDeleteTarget] = useState<Address | null>(null)

  function load() {
    setStatus('loading')
    addressesApi
      .list()
      .then((list) => {
        setAddresses(list)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(() => {
    load()
  }, [])

  function openAddForm() {
    setEditingId(null)
    setDraft(EMPTY_DRAFT)
    setFormOpen(true)
  }

  function openEditForm(address: Address) {
    setEditingId(address.id)
    setDraft({
      label: address.label,
      line1: address.line1,
      line2: address.line2 ?? '',
      city: address.city,
      state: address.state,
      postalCode: address.postalCode,
      lat: address.lat,
      lng: address.lng,
      isDefault: address.isDefault ?? false,
    })
    setFormOpen(true)
  }

  function update<K extends keyof NewAddress>(key: K, value: NewAddress[K]) {
    setDraft((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!draft.line1 || !draft.city || !draft.state || !draft.postalCode) return
    setSubmitting(true)
    setMessage(null)
    try {
      if (editingId) {
        const updated = await addressesApi.update(editingId, draft)
        setAddresses((prev) => prev.map((a) => (a.id === editingId ? updated : reconcileDefault(a, updated))))
        setMessage('Address updated.')
      } else {
        const created = await addressesApi.create(draft)
        setAddresses((prev) => [...prev.map((a) => reconcileDefault(a, created)), created])
        setMessage('Address added.')
      }
      setFormOpen(false)
    } catch {
      setMessage('Could not save this address. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  // When the server clears every other default in favour of `changed`, mirror that locally
  // without waiting for a full reload.
  function reconcileDefault(address: Address, changed: Address): Address {
    if (address.id === changed.id) return address
    return changed.isDefault ? { ...address, isDefault: false } : address
  }

  async function handleSetDefault(address: Address) {
    setBusyId(address.id)
    setMessage(null)
    try {
      const updated = await addressesApi.update(address.id, { ...toNewAddress(address), isDefault: true })
      setAddresses((prev) => prev.map((a) => (a.id === address.id ? updated : reconcileDefault(a, updated))))
    } catch {
      setMessage('Could not set this as your default address.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    const target = deleteTarget
    setBusyId(target.id)
    setMessage(null)
    try {
      await addressesApi.remove(target.id)
      setAddresses((prev) => prev.filter((a) => a.id !== target.id))
      setDeleteTarget(null)
      setMessage('Address removed.')
    } catch {
      setMessage('Could not remove this address.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="page-shell justify-center px-5 py-6">
    <div className="page-narrow flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold text-ink">Your addresses</h1>
        <button type="button" className="text-sm font-semibold text-brand-700" onClick={openAddForm}>
          + Add new
        </button>
      </div>

      {message && (
        <p role="status" className="text-sm text-brand-700">
          {message}
        </p>
      )}

      {status === 'loading' && (
        <div className="flex flex-col gap-3" aria-label="Loading your addresses">
          {[0, 1].map((key) => (
            <div key={key} className="card h-24 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState
          icon={<MapPinIcon className="h-12 w-12" />}
          title="Couldn't load your addresses"
          description="Check your connection and try again."
          action={
            <button type="button" className="btn-primary" onClick={load}>
              Retry
            </button>
          }
        />
      )}

      {status === 'success' && addresses.length === 0 && !formOpen && (
        <EmptyState
          icon={<MapPinIcon className="h-12 w-12" />}
          title="No saved addresses yet"
          description="Add an address so checkout is faster next time."
          action={
            <button type="button" className="btn-primary" onClick={openAddForm}>
              Add an address
            </button>
          }
        />
      )}

      {status === 'success' &&
        addresses.map((address) => (
          <div key={address.id} className="card flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <h2 className="font-bold text-ink">{address.label}</h2>
              {address.isDefault && (
                <span className="rounded-full bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-700">
                  Default
                </span>
              )}
            </div>
            <p className="text-sm text-ink-muted">
              {address.line1}
              {address.line2 ? `, ${address.line2}` : ''}, {address.city}, {address.state} {address.postalCode}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-ghost"
                disabled={busyId === address.id}
                onClick={() => openEditForm(address)}
              >
                Edit
              </button>
              {!address.isDefault && (
                <button
                  type="button"
                  className="btn-ghost"
                  disabled={busyId === address.id}
                  onClick={() => handleSetDefault(address)}
                >
                  Set as default
                </button>
              )}
              <button
                type="button"
                className="btn-ghost text-danger-500"
                disabled={busyId === address.id}
                onClick={() => setDeleteTarget(address)}
              >
                Delete
              </button>
            </div>
          </div>
        ))}

      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editingId ? 'Edit address' : 'Add a new address'}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <input
            className="input-field"
            placeholder="Label (e.g. Home)"
            value={draft.label}
            onChange={(event) => update('label', event.target.value)}
          />
          <input
            className="input-field"
            placeholder="Address line"
            value={draft.line1}
            onChange={(event) => update('line1', event.target.value)}
            required
          />
          <input
            className="input-field"
            placeholder="Apartment, floor, landmark (optional)"
            value={draft.line2 ?? ''}
            onChange={(event) => update('line2', event.target.value)}
          />
          <div className="flex gap-2">
            <input
              className="input-field"
              placeholder="City"
              value={draft.city}
              onChange={(event) => update('city', event.target.value)}
              required
            />
            <input
              className="input-field"
              placeholder="PIN code"
              value={draft.postalCode}
              onChange={(event) => update('postalCode', event.target.value)}
              required
            />
          </div>
          <input
            className="input-field"
            placeholder="State"
            value={draft.state}
            onChange={(event) => update('state', event.target.value)}
            required
          />
          <label className="flex items-center gap-2 text-sm text-ink-muted">
            <input
              type="checkbox"
              checked={draft.isDefault ?? false}
              onChange={(event) => update('isDefault', event.target.checked)}
            />
            Set as my default address
          </label>
          <div className="mt-1 flex gap-2">
            <button type="submit" className="btn-primary flex-1 py-2.5 text-sm" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save address'}
            </button>
            <button type="button" className="btn-ghost" onClick={() => setFormOpen(false)}>
              Cancel
            </button>
          </div>
        </form>
      </Dialog>

      <Dialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        title={`Delete ${deleteTarget?.label ?? 'this address'}?`}
        actions={
          <>
            <button
              type="button"
              className="btn-primary bg-danger-500 active:bg-danger-600"
              disabled={busyId === deleteTarget?.id}
              onClick={handleDelete}
            >
              Delete
            </button>
            <button type="button" className="btn-ghost" onClick={() => setDeleteTarget(null)}>
              Cancel
            </button>
          </>
        }
      >
        This can&apos;t be undone.
      </Dialog>
    </div>
    </div>
  )
}

function toNewAddress(address: Address): NewAddress {
  return {
    label: address.label,
    line1: address.line1,
    line2: address.line2,
    city: address.city,
    state: address.state,
    postalCode: address.postalCode,
    lat: address.lat,
    lng: address.lng,
    isDefault: address.isDefault,
  }
}
