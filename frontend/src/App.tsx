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
import Addresses from './pages/Addresses'
import Orders from './pages/Orders'
import OrderTracking from './pages/OrderTracking'
import Login from './pages/Login'
import Register from './pages/Register'
import VerifyEmail from './pages/VerifyEmail'
import RegisterSupermarketOwner from './pages/RegisterSupermarketOwner'
import AdminSupermarkets from './pages/AdminSupermarkets'
import AdminOrders from './pages/AdminOrders'
import MySupermarketStatus from './pages/MySupermarketStatus'
import MyStoreCatalogue from './pages/MyStoreCatalogue'
import MyStoreOrders from './pages/MyStoreOrders'

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
      </LocationProvider>
    </AuthProvider>
  )
}

export default App
