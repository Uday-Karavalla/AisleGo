import { Link } from 'react-router-dom'
import { adminCouponApi } from '../api/coupons'
import { CouponManager } from '../components/CouponManager'

export default function AdminCoupons() {
  return (
    <div className="page-wide flex flex-col gap-5 px-5 py-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-ink">Platform coupons</h1>
          <p className="mt-1 text-sm text-ink-muted">Manage discounts that shoppers can use at every supermarket.</p>
        </div>
        <Link to="/admin" className="shrink-0 text-sm font-semibold text-brand-700">Admin home</Link>
      </div>
      <CouponManager
        api={adminCouponApi}
        title="Platform-wide coupon codes"
        description="These codes apply at every store unless that store has its own coupon with the same code."
      />
    </div>
  )
}
