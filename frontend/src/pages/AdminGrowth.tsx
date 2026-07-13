import { useEffect, useState } from 'react'
import { analyticsApi } from '../api/growth'
import type { GrowthDashboard } from '../api/growth'

export default function AdminGrowth() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState<GrowthDashboard | null>(null)
  useEffect(() => { analyticsApi.dashboard(days).then(setData).catch(() => {}) }, [days])
  const cards = data ? [
    ['Visitors', data.visitors], ['Store views', data.storeViews], ['Searches', data.searches],
    ['Added to cart', data.addToCarts], ['Checkout starts', data.checkouts], ['Purchases', data.purchases],
    ['Coupon applies', data.couponApplications], ['Visitor conversion', `${data.checkoutConversionPercent}%`],
  ] : []
  const maxDaily = data ? Math.max(1, ...Object.values(data.dailyPurchases)) : 1
  return (
    <div className="page-wide flex flex-col gap-5 px-5 py-6">
      <div className="flex items-end justify-between"><div><h1 className="text-xl font-extrabold text-ink">Growth dashboard</h1><p className="text-sm text-ink-muted">First-party acquisition and checkout funnel.</p></div><select className="input-field w-auto" value={days} onChange={(e) => setDays(Number(e.target.value))}><option value={7}>7 days</option><option value={30}>30 days</option><option value={90}>90 days</option></select></div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">{cards.map(([label, value]) => <div key={label} className="card"><p className="text-xs font-semibold text-ink-faint">{label}</p><p className="mt-1 text-2xl font-black text-ink">{value}</p></div>)}</div>
      {data && Object.keys(data.dailyPurchases).length > 0 && <section className="card"><h2 className="text-sm font-bold text-ink">Purchases by day</h2><div className="mt-4 flex h-44 items-end gap-2">{Object.entries(data.dailyPurchases).map(([date, count]) => <div key={date} className="flex min-w-0 flex-1 flex-col items-center gap-1"><span className="text-xs font-bold">{count}</span><div className="w-full rounded-t bg-brand-500" style={{ height: `${Math.max(8, count / maxDaily * 120)}px` }} /><span className="w-full truncate text-center text-[9px] text-ink-faint">{date.slice(5)}</span></div>)}</div></section>}
    </div>
  )
}
