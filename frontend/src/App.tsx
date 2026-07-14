import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { LocationProvider } from './context/LocationContext'
import { CartProvider } from './context/CartContext'
import { FavoritesProvider } from './context/FavoritesContext'
import { Layout } from './components/Layout'
import { CrossStoreDialog } from './components/CrossStoreDialog'
import { ProtectedRoute } from './components/ProtectedRoute'
import Home from './pages/Home'
import StoreDiscovery from './pages/StoreDiscovery'
import Storefront from './pages/Storefront'
import CategoryBrowse from './pages/CategoryBrowse'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import Addresses from './pages/Addresses'
import Orders from './pages/Orders'
import OrderTracking from './pages/OrderTracking'
import Login from './pages/Login'
import Register from './pages/Register'
import VerifyEmail from './pages/VerifyEmail'
import RegisterSupermarketOwner from './pages/RegisterSupermarketOwner'
import AdminSupermarkets from './pages/AdminSupermarkets'
import AdminOrders from './pages/AdminOrders'
import AdminUsers from './pages/AdminUsers'
import AdminCoupons from './pages/AdminCoupons'
import MySupermarketStatus from './pages/MySupermarketStatus'
import MyStoreCatalogue from './pages/MyStoreCatalogue'
import MyStoreOrders from './pages/MyStoreOrders'
import Terms from './pages/legal/Terms'
import PrivacyPolicy from './pages/legal/PrivacyPolicy'
import RefundPolicy from './pages/legal/RefundPolicy'
import Referrals from './pages/Referrals'
import Notifications from './pages/Notifications'
import AdminGrowth from './pages/AdminGrowth'
import ProductDetail from './pages/ProductDetail'
import LocalDelivery from './pages/LocalDelivery'
import RegisterDeliveryPartner from './pages/RegisterDeliveryPartner'
import DeliveryPartnerDashboard from './pages/DeliveryPartnerDashboard'
import AdminDeliveryPartners from './pages/AdminDeliveryPartners'
import { GrowthTracker } from './components/GrowthTracker'

function App() {
  return (
    <AuthProvider>
      <LocationProvider>
        <FavoritesProvider>
          <CartProvider>
          <BrowserRouter>
            <GrowthTracker />
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<Home />} />
                <Route path="/stores" element={<StoreDiscovery />} />
                <Route path="/stores/:storeId" element={<Storefront />} />
                <Route path="/stores/:storeId/products/:productId" element={<ProductDetail />} />
                <Route path="/category/:category" element={<CategoryBrowse />} />
                <Route path="/delivery/:city" element={<LocalDelivery />} />
                <Route path="/cart" element={<Cart />} />
                <Route
                  path="/checkout"
                  element={
                    <ProtectedRoute requiredRole="CUSTOMER">
                      <Checkout />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/orders"
                  element={
                    <ProtectedRoute requiredRole="CUSTOMER">
                      <Orders />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/addresses"
                  element={
                    <ProtectedRoute requiredRole="CUSTOMER">
                      <Addresses />
                    </ProtectedRoute>
                  }
                />
                <Route path="/orders/:orderId" element={<OrderTracking />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                  path="/verify-email"
                  element={
                    <ProtectedRoute>
                      <VerifyEmail />
                    </ProtectedRoute>
                  }
                />
                <Route path="/register-store" element={<RegisterSupermarketOwner />} />
                <Route path="/register-delivery-partner" element={<RegisterDeliveryPartner />} />
                <Route path="/deliveries" element={<ProtectedRoute requiredRole="DELIVERY_PARTNER"><DeliveryPartnerDashboard /></ProtectedRoute>} />
                <Route
                  path="/referrals"
                  element={
                    <ProtectedRoute requiredRole="CUSTOMER">
                      <Referrals />
                    </ProtectedRoute>
                  }
                />
                <Route path="/notifications" element={<ProtectedRoute><Notifications /></ProtectedRoute>} />
                <Route path="/legal/terms" element={<Terms />} />
                <Route path="/legal/privacy" element={<PrivacyPolicy />} />
                <Route path="/legal/refunds" element={<RefundPolicy />} />
                <Route
                  path="/admin"
                  element={
                    <ProtectedRoute requiredRole="ADMIN">
                      <AdminSupermarkets />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/orders"
                  element={
                    <ProtectedRoute requiredRole="ADMIN">
                      <AdminOrders />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/users"
                  element={
                    <ProtectedRoute requiredRole="ADMIN">
                      <AdminUsers />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/coupons"
                  element={
                    <ProtectedRoute requiredRole="ADMIN">
                      <AdminCoupons />
                    </ProtectedRoute>
                  }
                />
                <Route path="/admin/growth" element={<ProtectedRoute requiredRole="ADMIN"><AdminGrowth /></ProtectedRoute>} />
                <Route path="/admin/delivery-partners" element={<ProtectedRoute requiredRole="ADMIN"><AdminDeliveryPartners /></ProtectedRoute>} />
                <Route
                  path="/my-store"
                  element={
                    <ProtectedRoute requiredRole="SUPERMARKET_OWNER">
                      <MySupermarketStatus />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/my-store/catalogue"
                  element={
                    <ProtectedRoute requiredRole="SUPERMARKET_OWNER">
                      <MyStoreCatalogue />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/my-store/orders"
                  element={
                    <ProtectedRoute requiredRole="SUPERMARKET_OWNER">
                      <MyStoreOrders />
                    </ProtectedRoute>
                  }
                />
              </Route>
            </Routes>
          </BrowserRouter>
          {/* Mounted once so the cross-store conflict rule can be enforced from any page. */}
          <CrossStoreDialog />
          </CartProvider>
        </FavoritesProvider>
      </LocationProvider>
    </AuthProvider>
  )
}

export default App
