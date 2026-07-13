import { useEffect, useState } from 'react'
import { growthApi } from '../api/growth'
import type { ReferralSummary } from '../api/growth'
import { ShareIcon, TagIcon } from '../components/icons'
import { trackEvent } from '../api/growth'

export default function Referrals() {
  const [summary, setSummary] = useState<ReferralSummary | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    growthApi.referralSummary().then(setSummary).catch(() => setMessage('Could not load your referral rewards.'))
  }, [])

  const inviteUrl = summary
    ? `${window.location.origin}/register?ref=${encodeURIComponent(summary.referralCode)}`
    : ''
  const inviteText = summary
    ? `Shop groceries from nearby stores with AisleGo. Join with my code ${summary.referralCode} and get ₹100 off your first order: ${inviteUrl}`
    : ''

  async function share() {
    if (!summary) return
    trackEvent('share', { channel: navigator.share ? 'native' : 'clipboard', referral: true })
    if (navigator.share) {
      await navigator.share({ title: 'Try AisleGo', text: inviteText, url: inviteUrl }).catch(() => {})
    } else {
      await navigator.clipboard.writeText(inviteText)
      setMessage('Invite copied to your clipboard.')
    }
  }

  return (
    <div className="page-narrow flex flex-col gap-5 px-5 py-6">
      <div className="rounded-3xl bg-gradient-to-br from-brand-700 to-brand-500 p-6 text-white shadow-pop">
        <TagIcon className="h-9 w-9" />
        <h1 className="mt-3 text-2xl font-extrabold">Give ₹100, get ₹100</h1>
        <p className="mt-2 text-sm text-white/85">
          Your friend receives a welcome offer. When their first payment succeeds, both of you receive a private ₹100 coupon.
        </p>
      </div>

      {summary && (
        <>
          <div className="card text-center">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-faint">Your referral code</p>
            <p className="mt-2 text-2xl font-black tracking-widest text-brand-700">{summary.referralCode}</p>
            <button type="button" className="btn-primary mt-4 w-full" onClick={share}>
              <ShareIcon className="h-4 w-4" /> Share invite
            </button>
            <a
              className="btn-secondary mt-2 w-full"
              href={`https://wa.me/?text=${encodeURIComponent(inviteText)}`}
              target="_blank"
              rel="noreferrer"
            >
              Share on WhatsApp
            </a>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="card text-center"><p className="text-2xl font-black text-ink">{summary.invitedFriends}</p><p className="text-xs text-ink-muted">Friends joined</p></div>
            <div className="card text-center"><p className="text-2xl font-black text-ink">{summary.rewardedFriends}</p><p className="text-xs text-ink-muted">Rewards unlocked</p></div>
          </div>

          {summary.rewardCouponCodes.length > 0 && (
            <section>
              <h2 className="mb-2 text-sm font-bold text-ink">Your active reward codes</h2>
              <div className="flex flex-wrap gap-2">
                {summary.rewardCouponCodes.map((code) => <span key={code} className="rounded-xl bg-brand-50 px-3 py-2 text-sm font-bold text-brand-700">{code}</span>)}
              </div>
            </section>
          )}
        </>
      )}
      {message && <p role="status" className="text-sm text-ink-muted">{message}</p>}
    </div>
  )
}
