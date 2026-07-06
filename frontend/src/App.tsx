import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { LocationProvider } from './context/LocationContext'
import { CartProvider } from './context/CartContext'
import { Layout } from './components/Layout'
import { CrossStoreDialog } from './components/CrossStoreDialog'
import { ProtectedRoute } from './components/ProtectedRoute'
import Home from './pages/Home'
import StoreDiscovery from './pages/StoreDiscovery'
import Storefront from './pages/Storefront'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import OrderTracking from './pages/OrderTracking'
import Login from './pages/Login'
import RegisterSupermarketOwner from './pages/RegisterSupermarketOwner'
import AdminSupermarkets from './pages/AdminSupermarkets'
import MySupermarketStatus from './pages/MySupermarketStatus'
import MyStoreCatalogue from './pages/MyStoreCatalogue'

function App() {
  return (
    <AuthProvider>
      <LocationProvider>
        <CartProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<Home />} />
                <Route path="/stores" element={<StoreDiscovery />} />
                <Route path="/stores/:storeId" element={<Storefront />} />
                <Route path="/cart" element={<Cart />} />
                <Route path="/checkout" element={<Checkout />} />
                <Route path="/orders/:orderId" element={<OrderTracking />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register-store" element={<RegisterSupermarketOwner />} />
                <Route
                  path="/admin"
                  element={
                    <ProtectedRoute requiredRole="ADMIN">
                      <AdminSupermarkets />
                    </ProtectedRoute>
                  }
                />
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
              </Route>
            </Routes>
          </BrowserRouter>
          {/* Mounted once so the cross-store conflict rule can be enforced from any page. */}
          <CrossStoreDialog />
        </CartProvider>
      </LocationProvider>
    </AuthProvider>
  )
}

export default App
