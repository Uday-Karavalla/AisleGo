import { useEffect, useState } from 'react'
import { deliveryPartnerApi } from '../api/deliveryPartner'
import type { DeliveryPartnerProfile } from '../api/deliveryPartner'
import type { DeliveryOffer } from '../api/deliveryPartner'
import type { DeliveryEarnings, DeliveryHistoryItem } from '../api/deliveryPartner'

export default function DeliveryPartnerDashboard() {
  const [profile, setProfile] = useState<DeliveryPartnerProfile | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [offers, setOffers] = useState<DeliveryOffer[]>([])
  const [acceptingId, setAcceptingId] = useState<number | null>(null)
  const [acceptedOffer, setAcceptedOffer] = useState<DeliveryOffer | null>(null)
  const [handoffCode, setHandoffCode] = useState('')
  const [locationMessage, setLocationMessage] = useState<string | null>(null)
  const [earnings, setEarnings] = useState<DeliveryEarnings | null>(null)
  const [history, setHistory] = useState<DeliveryHistoryItem[]>([])

  useEffect(() => {
    deliveryPartnerApi.me().then((loaded) => {
      setProfile(loaded)
      deliveryPartnerApi.activeDelivery().then(setAcceptedOffer)
      if (loaded.available) deliveryPartnerApi.listOffers().then(setOffers)
    }).catch(() => setError('Could not load your delivery profile.'))
    deliveryPartnerApi.earnings().then(setEarnings).catch(() => {})
    deliveryPartnerApi.history().then(setHistory).catch(() => {})
  }, [])

  useEffect(() => {
    if (acceptedOffer?.status !== 'OUT_FOR_DELIVERY') {
      setLocationMessage(null)
      return
    }
    if (!navigator.geolocation) {
      setLocationMessage('This device does not support live location sharing.')
      return
    }
    setLocationMessage('Sharing your live location with this customer.')
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        void deliveryPartnerApi.updateLocation(acceptedOffer.orderId,
          position.coords.latitude, position.coords.longitude)
          .then(() => setLocationMessage('Live location shared.'))
          .catch(() => setLocationMessage('Could not send your latest location.'))
      },
      (locationError) => setLocationMessage(locationError.code === locationError.PERMISSION_DENIED
        ? 'Location permission is required while delivering.'
        : 'Could not read your current location.'),
      { enableHighAccuracy: true, maximumAge: 10_000, timeout: 15_000 },
    )
    return () => navigator.geolocation.clearWatch(watchId)
  }, [acceptedOffer?.orderId, acceptedOffer?.status])

  async function toggleAvailability() {
    if (!profile) return
    setSaving(true)
    setError(null)
    try {
      const updated = await deliveryPartnerApi.updateAvailability(!profile.available)
      setProfile(updated)
      setOffers(updated.available ? await deliveryPartnerApi.listOffers() : [])
    } catch {
      setError('Could not update your availability.')
    } finally {
      setSaving(false)
    }
  }

  async function acceptOffer(offer: DeliveryOffer) {
    setAcceptingId(offer.orderId)
    setError(null)
    try {
      const accepted = await deliveryPartnerApi.acceptOffer(offer.orderId)
      setAcceptedOffer(accepted)
      setOffers([])
      setProfile((current) => current ? { ...current, available: false } : current)
    } catch {
      setError('That offer is no longer available. Refreshing offers…')
      setOffers(await deliveryPartnerApi.listOffers().catch(() => []))
    } finally {
      setAcceptingId(null)
    }
  }

  async function advanceDelivery() {
    if (!acceptedOffer) return
    setSaving(true)
    setError(null)
    try {
      let updated: DeliveryOffer
      if (acceptedOffer.status === 'DELIVERY_PARTNER_ASSIGNED') {
        updated = await deliveryPartnerApi.verifyPickup(acceptedOffer.orderId, handoffCode)
      } else if (acceptedOffer.status === 'PICKED_UP') {
        updated = await deliveryPartnerApi.startDelivery(acceptedOffer.orderId)
      } else {
        updated = await deliveryPartnerApi.verifyDelivery(acceptedOffer.orderId, handoffCode)
      }
      setHandoffCode('')
      if (updated.status === 'DELIVERED') {
        setAcceptedOffer(null)
        setProfile((current) => current ? { ...current, available: true } : current)
        setOffers(await deliveryPartnerApi.listOffers())
        setEarnings(await deliveryPartnerApi.earnings())
        setHistory(await deliveryPartnerApi.history())
      } else {
        setAcceptedOffer(updated)
      }
    } catch {
      setError('The code is incorrect or this delivery can no longer be updated.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-5 px-5 py-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">Delivery partner</p>
        <h1 className="text-xl font-extrabold text-ink">Your deliveries</h1>
      </div>
      {error && <p role="alert" className="text-sm text-danger-500">{error}</p>}
      {!profile && !error && <div className="card h-28 animate-pulse bg-black/5" aria-label="Loading profile" />}
      {profile && (
        <div className="card flex items-center justify-between gap-4">
          <div>
            <h2 className="font-bold text-ink">{profile.fullName}</h2>
            <p className="text-sm text-ink-muted">{profile.status === 'PENDING' ? 'Application awaiting admin review'
              : profile.status === 'REJECTED' ? `Application rejected: ${profile.rejectionReason ?? 'No reason provided'}`
                : profile.available ? 'Online and ready for offers' : 'Offline'}</p>
          </div>
          <button type="button" className={profile.available ? 'btn-secondary' : 'btn-primary'} disabled={saving || profile.status !== 'VERIFIED'} onClick={toggleAvailability}>
            {saving ? 'Updating…' : profile.available ? 'Go offline' : 'Go online'}
          </button>
        </div>
      )}
      {profile?.status === 'VERIFIED' && earnings && (
        <div className="grid grid-cols-3 gap-2">
          <div className="card p-3 text-center"><p className="text-xs text-ink-faint">Today</p><p className="font-bold text-ink">{earnings.currency} {earnings.today.toFixed(2)}</p></div>
          <div className="card p-3 text-center"><p className="text-xs text-ink-faint">Total</p><p className="font-bold text-ink">{earnings.currency} {earnings.total.toFixed(2)}</p></div>
          <div className="card p-3 text-center"><p className="text-xs text-ink-faint">Deliveries</p><p className="font-bold text-ink">{earnings.completedDeliveries}</p></div>
        </div>
      )}
      {acceptedOffer && (
        <div className="card flex flex-col gap-2 border-2 border-brand-600/20">
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">Accepted delivery</p>
          <h2 className="font-bold text-ink">Order #{acceptedOffer.orderId} · {acceptedOffer.supermarketName}</h2>
          <p className="text-sm text-ink-muted">Pickup: {acceptedOffer.pickupAddress ?? acceptedOffer.branchName}</p>
          <p className="text-sm text-ink-muted">Deliver to: {acceptedOffer.deliveryAddress}</p>
          <p className="text-sm font-semibold text-brand-700">
            {acceptedOffer.status === 'DELIVERY_PARTNER_ASSIGNED' ? 'Waiting for store pickup code'
              : acceptedOffer.status === 'PICKED_UP' ? 'Pickup verified'
                : 'Out for delivery — ask the customer for their delivery code'}
          </p>
          {locationMessage && <p role="status" className="text-xs text-ink-muted">{locationMessage}</p>}
          {acceptedOffer.status !== 'PICKED_UP' && (
            <input className="input-field" inputMode="numeric" pattern="[0-9]{6}" maxLength={6}
              aria-label={acceptedOffer.status === 'DELIVERY_PARTNER_ASSIGNED' ? 'Pickup code' : 'Delivery code'}
              placeholder="6-digit code" value={handoffCode}
              onChange={(event) => setHandoffCode(event.target.value.replace(/\D/g, ''))} />
          )}
          <button type="button" className="btn-primary" disabled={saving || (acceptedOffer.status !== 'PICKED_UP' && handoffCode.length !== 6)} onClick={advanceDelivery}>
            {saving ? 'Updating…' : acceptedOffer.status === 'DELIVERY_PARTNER_ASSIGNED' ? 'Verify pickup'
              : acceptedOffer.status === 'PICKED_UP' ? 'Start delivery' : 'Complete delivery'}
          </button>
        </div>
      )}
      {profile?.available && offers.length === 0 && <div className="card text-center text-sm text-ink-muted">No delivery offers yet.</div>}
      {profile?.available && offers.map((offer) => (
        <div key={offer.orderId} className="card flex flex-col gap-2">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">Order #{offer.orderId}</p>
              <h2 className="font-bold text-ink">{offer.supermarketName}</h2>
              <p className="text-sm text-ink-muted">{offer.branchName} · {offer.itemCount} items</p>
            </div>
            <p className="font-bold text-ink">{offer.currency} {offer.orderTotal.toFixed(2)}</p>
          </div>
          <p className="text-sm text-ink-muted">Pickup: {offer.pickupAddress ?? offer.branchName}</p>
          <p className="text-sm text-ink-muted">Deliver to: {offer.deliveryAddress}</p>
          {offer.scheduledFor && <p className="text-sm text-ink-muted">Scheduled: {new Date(offer.scheduledFor).toLocaleString()}</p>}
          <button type="button" className="btn-primary" disabled={acceptingId !== null} onClick={() => acceptOffer(offer)}>
            {acceptingId === offer.orderId ? 'Accepting…' : 'Accept delivery'}
          </button>
        </div>
      ))}
      {profile?.status === 'VERIFIED' && (
        <div className="flex flex-col gap-2">
          <h2 className="font-bold text-ink">Delivery history</h2>
          {history.length === 0 && <div className="card text-sm text-ink-muted">No completed deliveries yet.</div>}
          {history.map((item) => (
            <div key={item.orderId} className="card flex items-center justify-between gap-3">
              <div><p className="font-semibold text-ink">Order #{item.orderId} · {item.supermarketName}</p><p className="text-xs text-ink-muted">{item.branchName} · {new Date(item.deliveredAt).toLocaleString()}</p></div>
              <p className="font-bold text-brand-700">{item.currency} {item.earning.toFixed(2)}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
