import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import type { Coupon, CouponCrudApi, DiscountType } from '../api/coupons'
import { Dialog } from './Dialog'

interface CouponManagerProps {
  api: CouponCrudApi
  title: string
  description: string
}

interface CouponDraft {
  code: string
  discountType: DiscountType
  discountValue: string
  expiresAt: string
  active: boolean
}

const EMPTY_DRAFT: CouponDraft = {
  code: '',
  discountType: 'PERCENTAGE',
  discountValue: '',
  expiresAt: '',
  active: true,
}

function toLocalDateTime(value: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

function toDraft(coupon: Coupon): CouponDraft {
  return {
    code: coupon.code,
    discountType: coupon.discountType,
    discountValue: String(coupon.discountType === 'PERCENTAGE' ? coupon.percentOff : coupon.amountOff),
    expiresAt: toLocalDateTime(coupon.expiresAt),
    active: coupon.active,
  }
}

function discountLabel(coupon: Coupon): string {
  return coupon.discountType === 'PERCENTAGE'
    ? `${coupon.percentOff}% off`
    : `${coupon.currency ?? 'INR'} ${coupon.amountOff?.toFixed(2)} off`
}

export function CouponManager({ api, title, description }: CouponManagerProps) {
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [draft, setDraft] = useState<CouponDraft>(EMPTY_DRAFT)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<Coupon | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [messageIsError, setMessageIsError] = useState(false)

  function showMessage(text: string, isError = false) {
    setMessage(text)
    setMessageIsError(isError)
  }

  function load() {
    setLoading(true)
    setLoadError(false)
    api
      .list()
      .then(setCoupons)
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // The adapter is a module-level constant in both callers.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api])

  function buildTerms() {
    const value = Number(draft.discountValue)
    if (!Number.isFinite(value) || value <= 0) {
      throw new Error('Enter a discount greater than zero.')
    }
    if (draft.discountType === 'PERCENTAGE' && (!Number.isInteger(value) || value > 100)) {
      throw new Error('Percentage discounts must be a whole number from 1 to 100.')
    }
    return {
      discountType: draft.discountType,
      percentOff: draft.discountType === 'PERCENTAGE' ? value : null,
      amountOff: draft.discountType === 'FLAT' ? value : null,
      currency: draft.discountType === 'FLAT' ? 'INR' : null,
      expiresAt: draft.expiresAt ? new Date(draft.expiresAt).toISOString() : null,
    }
  }

  function closeForm() {
    setShowCreate(false)
    setEditingId(null)
    setDraft(EMPTY_DRAFT)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setMessage(null)
    try {
      const terms = buildTerms()
      if (editingId !== null) {
        const updated = await api.update(editingId, { ...terms, active: draft.active })
        setCoupons((current) => current.map((coupon) => (coupon.id === updated.id ? updated : coupon)))
        showMessage(`${updated.code} updated.`)
      } else {
        const code = draft.code.trim().toUpperCase()
        if (!code) throw new Error('Enter a coupon code.')
        const created = await api.create({ code, ...terms })
        setCoupons((current) => [created, ...current])
        showMessage(`${created.code} created.`)
      }
      closeForm()
    } catch (error) {
      showMessage(error instanceof Error ? error.message : 'Could not save the coupon.', true)
    } finally {
      setSubmitting(false)
    }
  }

  function startEdit(coupon: Coupon) {
    setShowCreate(false)
    setEditingId(coupon.id)
    setDraft(toDraft(coupon))
    setMessage(null)
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setSubmitting(true)
    setMessage(null)
    try {
      await api.remove(deleteTarget.id)
      setCoupons((current) => current.filter((coupon) => coupon.id !== deleteTarget.id))
      showMessage(`${deleteTarget.code} deleted.`)
      setDeleteTarget(null)
    } catch (error) {
      showMessage(error instanceof Error ? error.message : 'Could not delete the coupon.', true)
    } finally {
      setSubmitting(false)
    }
  }

  const formOpen = showCreate || editingId !== null

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold text-ink">{title}</h2>
          <p className="mt-1 text-xs text-ink-muted">{description}</p>
        </div>
        {!formOpen && (
          <button
            type="button"
            className="shrink-0 text-xs font-semibold text-brand-700"
            onClick={() => {
              setDraft(EMPTY_DRAFT)
              setShowCreate(true)
              setMessage(null)
            }}
          >
            + Add coupon
          </button>
        )}
      </div>

      {message && (
        <p role={messageIsError ? 'alert' : 'status'} className={`text-sm ${messageIsError ? 'text-danger-500' : 'text-brand-700'}`}>
          {message}
        </p>
      )}

      {formOpen && (
        <form onSubmit={handleSubmit} className="card flex flex-col gap-2">
          <input
            className="input-field uppercase"
            placeholder="Coupon code"
            maxLength={32}
            value={draft.code}
            onChange={(event) => setDraft((current) => ({ ...current, code: event.target.value }))}
            disabled={editingId !== null}
            required
          />
          <div className="flex gap-2">
            <select
              className="input-field"
              value={draft.discountType}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  discountType: event.target.value as DiscountType,
                  discountValue: '',
                }))
              }
            >
              <option value="PERCENTAGE">Percentage off</option>
              <option value="FLAT">Flat amount off</option>
            </select>
            <input
              className="input-field"
              type="number"
              min={draft.discountType === 'PERCENTAGE' ? 1 : 0.01}
              max={draft.discountType === 'PERCENTAGE' ? 100 : undefined}
              step={draft.discountType === 'PERCENTAGE' ? 1 : 0.01}
              placeholder={draft.discountType === 'PERCENTAGE' ? 'Percent' : 'Amount (INR)'}
              value={draft.discountValue}
              onChange={(event) => setDraft((current) => ({ ...current, discountValue: event.target.value }))}
              required
            />
          </div>
          <label className="flex flex-col gap-1 text-xs font-semibold text-ink-muted">
            Expires (optional)
            <input
              className="input-field font-normal"
              type="datetime-local"
              value={draft.expiresAt}
              onChange={(event) => setDraft((current) => ({ ...current, expiresAt: event.target.value }))}
            />
          </label>
          {editingId !== null && (
            <label className="flex items-center gap-2 text-xs text-ink-muted">
              <input
                type="checkbox"
                checked={draft.active}
                onChange={(event) => setDraft((current) => ({ ...current, active: event.target.checked }))}
              />
              Active and available to shoppers
            </label>
          )}
          <div className="flex gap-2">
            <button type="submit" className="btn-primary flex-1" disabled={submitting}>
              {submitting ? 'Saving…' : editingId !== null ? 'Save changes' : 'Create coupon'}
            </button>
            <button type="button" className="btn-secondary flex-1" onClick={closeForm} disabled={submitting}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading && <div className="card h-20 animate-pulse bg-black/5" aria-label="Loading coupons" />}
      {!loading && loadError && (
        <div className="card flex items-center justify-between gap-3 text-sm text-danger-500">
          <span>Couldn&apos;t load coupons.</span>
          <button type="button" className="btn-secondary px-3 py-1 text-xs" onClick={load}>Retry</button>
        </div>
      )}
      {!loading && !loadError && coupons.length === 0 && !formOpen && (
        <p className="text-sm text-ink-muted">No coupons yet.</p>
      )}
      {!loading && !loadError && coupons.map((coupon) => (
        <div key={coupon.id} className="card flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-bold text-ink">{coupon.code}</span>
              <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                coupon.active ? 'bg-brand-50 text-brand-700' : 'bg-danger-50 text-danger-500'
              }`}>
                {coupon.active ? 'Active' : 'Inactive'}
              </span>
            </div>
            <p className="text-sm text-ink-muted">{discountLabel(coupon)}</p>
            <p className="text-xs text-ink-faint">
              {coupon.expiresAt ? `Expires ${new Date(coupon.expiresAt).toLocaleString()}` : 'No expiry'}
            </p>
          </div>
          <div className="flex shrink-0 gap-2">
            <button type="button" className="rounded-full bg-surface-muted px-3 py-1 text-xs font-semibold text-ink" onClick={() => startEdit(coupon)}>
              Edit
            </button>
            <button type="button" className="rounded-full bg-danger-50 px-3 py-1 text-xs font-semibold text-danger-500" onClick={() => setDeleteTarget(coupon)}>
              Delete
            </button>
          </div>
        </div>
      ))}

      <Dialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        title={`Delete ${deleteTarget?.code ?? 'this coupon'}?`}
        actions={
          <>
            <button type="button" className="btn-primary bg-danger-500 active:bg-danger-600" onClick={handleDelete} disabled={submitting}>
              {submitting ? 'Deleting…' : 'Delete coupon'}
            </button>
            <button type="button" className="btn-ghost" onClick={() => setDeleteTarget(null)} disabled={submitting}>Cancel</button>
          </>
        }
      >
        Shoppers who already entered this code will lose the discount on their next cart refresh.
      </Dialog>
    </section>
  )
}
