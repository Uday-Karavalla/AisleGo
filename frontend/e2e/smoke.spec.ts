import { expect, test } from '@playwright/test'

const order = {
  id: 9001,
  supermarketId: 10,
  branchId: 101,
  status: 'PAYMENT_CONFIRMED',
  fulfilmentType: 'IMMEDIATE',
  scheduledFor: null,
  subtotal: 58,
  deliveryFee: 25,
  totalAmount: 83,
  currency: 'INR',
  couponCode: null,
  discountAmount: 0,
  items: [{ productId: 501, productName: 'Fresh Bananas', quantity: 1, unitPrice: 58, lineTotal: 58 }],
  deliveryAddress: '12 Market Road, Madanapalle 517325',
  createdAt: '2026-07-15T08:00:00Z',
}

async function stubCustomerJourney(page: import('@playwright/test').Page) {
  const checkoutIdempotencyKeys: string[] = []
  const cart = {
    id: 77,
    supermarketId: 10,
    items: [{ id: 701, productId: 501, productName: 'Fresh Bananas', quantity: 1, unitPrice: 58, lineTotal: 58, currency: 'INR' }],
    subtotal: 58,
    deliveryFee: 25,
    couponCode: null,
    discount: 0,
    total: 83,
  }

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    let body: unknown

    if (!path.startsWith('/api/')) {
      await route.continue()
      return
    }

    if (path === '/api/checkout' || path === '/api/checkout/9001/payment/verify') {
      checkoutIdempotencyKeys.push(request.headers()['idempotency-key'] ?? '')
    }

    if (path === '/api/stores/geocode') body = { lat: 13.55, lng: 78.5 }
    else if (path === '/api/stores/nearby') body = [{ branchId: 101, branchName: 'AisleGo Test Market', addressLine: '12 Market Road', city: 'Madanapalle', latitude: 13.55, longitude: 78.5, supermarketId: 10, supermarketName: 'AisleGo Test Market', distanceKm: 1.2, etaMinutes: 12, isOpen: true, rating: 4.7, ratingCount: 25 }]
    else if (path === '/api/stores/branches/101') body = { branchId: 101, branchName: 'AisleGo Test Market', addressLine: '12 Market Road', city: 'Madanapalle', isOpen: true, supermarketId: 10, supermarketName: 'AisleGo Test Market', logoUrl: null, rating: 4.7, ratingCount: 25 }
    else if (path === '/api/stores/10/categories') body = { categories: ['Fruit'] }
    else if (path === '/api/stores/10/products') body = { content: [{ id: 501, supermarketId: 10, name: 'Fresh Bananas', description: 'Locally sourced', sku: 'BAN-1', price: 58, currency: 'INR', categoryName: 'Fruit', imageUrl: null }], number: 0, totalPages: 1, totalElements: 1 }
    else if (path === '/api/stores/10/reviews') body = { averageRating: 4.7, reviewCount: 25, reviews: [] }
    else if (path === '/api/auth/login' && method === 'POST') body = { accessToken: 'e2e-token', refreshToken: 'e2e-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 }
    else if (path === '/api/auth/me') body = { id: 42, email: 'shopper@example.com', roles: ['CUSTOMER'], emailVerified: true }
    else if (path === '/api/addresses') body = [{ id: '301', label: 'Home', line1: '12 Market Road', city: 'Madanapalle', state: 'Andhra Pradesh', postalCode: '517325', isDefault: true }]
    else if (path === '/api/checkout' && method === 'POST') body = { order, payment: { provider: 'MOCK', requiresClientAction: false, gatewayOrderId: null, providerKeyId: null, amountMinorUnits: 8300, currency: 'INR' } }
    else if (path === '/api/checkout/9001/payment/verify') body = order
    else if (path === '/api/orders/9001') body = order
    else if (path === '/api/orders/9001/status') body = { status: 'PAYMENT_CONFIRMED' }
    else if (path === '/api/cart' || path === '/api/cart/items') body = cart
    else if (path === '/api/cart/coupons') body = []
    else if (path === '/api/favorites') body = { productIds: [], supermarketIds: [] }
    else body = {}

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  })

  return checkoutIdempotencyKeys
}

test.describe('public application smoke tests', () => {
  test('loads the shopping home page', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveTitle(/AisleGo/i)
    await expect(page.getByRole('heading', { name: /Groceries you trust/i })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Find stores near me' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'List your store' })).toBeVisible()
  })

  test('opens the manual address form', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Enter address manually' }).click()

    await expect(page.getByLabel('Delivery address')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Continue' })).toBeDisabled()
  })

  test('redirects a signed-out customer from checkout to sign in', async ({ page }) => {
    await page.goto('/checkout')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
    await expect(page.getByText('Your cart is waiting.')).toBeVisible()
  })

  test('keeps legal pages publicly accessible', async ({ page }) => {
    await page.goto('/legal/privacy')

    await expect(page).toHaveURL(/\/legal\/privacy$/)
    await expect(page.getByRole('heading', { name: /Privacy/i }).first()).toBeVisible()
  })

  test('completes the customer purchase journey with mock payment', async ({ page }) => {
    const checkoutIdempotencyKeys = await stubCustomerJourney(page)
    await page.goto('/')

    await page.getByRole('button', { name: 'Enter address manually' }).click()
    await page.getByLabel('Delivery address').fill('Madanapalle')
    await page.getByRole('button', { name: 'Continue' }).click()
    await expect(page.getByRole('heading', { name: 'Supermarkets around you' })).toBeVisible()

    await page.getByRole('button', { name: 'Open AisleGo Test Market' }).click()
    await expect(page.getByRole('heading', { name: 'AisleGo Test Market' })).toBeVisible()
    await page.getByRole('button', { name: 'Add Fresh Bananas to cart' }).click()
    await page.locator('nav[aria-label="Primary"]').getByRole('link', { name: /Cart/ }).click()

    await expect(page.getByText('Fresh Bananas')).toBeVisible()
    await page.getByRole('button', { name: 'Proceed to checkout' }).click()
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()

    await page.getByLabel('Email').fill('shopper@example.com')
    await page.getByLabel('Password').fill('safe-test-password')
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible()
    await expect(page.getByText('12 Market Road, Madanapalle 517325')).toBeVisible()

    await page.getByRole('button', { name: 'Pay & place order' }).click()
    await expect(page).toHaveURL(/\/orders\/9001$/)
    await expect(page.getByRole('heading', { name: 'Track your order' })).toBeVisible()
    await expect(page.getByText('1 × Fresh Bananas')).toBeVisible()
    expect(checkoutIdempotencyKeys).toHaveLength(2)
    expect(checkoutIdempotencyKeys[0]).not.toBe('')
    expect(checkoutIdempotencyKeys[1]).toBe(checkoutIdempotencyKeys[0])
  })

  test('keeps checkout retryable when the Razorpay widget is cancelled', async ({ page }) => {
    await page.route('https://checkout.razorpay.com/v1/checkout.js', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/javascript',
        body: 'window.Razorpay = class { constructor(options) { this.options = options } open() { this.options.modal?.ondismiss?.() } }',
      })
    })

    await page.addInitScript(() => {
      localStorage.setItem('aislego.authToken', 'customer-token')
      localStorage.setItem('aislego.cart', JSON.stringify({
        id: 'local-cart',
        storeId: '101',
        storeName: 'AisleGo Test Market',
        items: [{ id: '701', productId: '501', storeId: '101', storeName: 'AisleGo Test Market', name: 'Fresh Bananas', price: 58, unit: '1 kg', quantity: 1, allowSubstitution: true }],
        subtotal: 58,
        deliveryFee: 25,
        discount: 0,
        total: 83,
        couponCode: null,
      }))

    })

    await page.route('**/api/**', async (route) => {
      const request = route.request()
      const path = new URL(request.url()).pathname
      if (!path.startsWith('/api/')) {
        await route.continue()
        return
      }

      let body: unknown = {}
      if (path === '/api/auth/me') body = { id: 42, email: 'shopper@example.com', roles: ['CUSTOMER'], emailVerified: true }
      else if (path === '/api/addresses') body = [{ id: '301', label: 'Home', line1: '12 Market Road', city: 'Madanapalle', state: 'Andhra Pradesh', postalCode: '517325', isDefault: true }]
      else if (path === '/api/cart') body = { id: 77, supermarketId: 10, items: [{ id: 701, productId: 501, productName: 'Fresh Bananas', quantity: 1, unitPrice: 58, lineTotal: 58, currency: 'INR' }], subtotal: 58, deliveryFee: 25, couponCode: null, discount: 0, total: 83 }
      else if (path === '/api/checkout') body = { order, payment: { provider: 'RAZORPAY', requiresClientAction: true, gatewayOrderId: 'order_test_9001', providerKeyId: 'rzp_test_public', amountMinorUnits: 8300, currency: 'INR' } }
      else if (path === '/api/favorites') body = { productIds: [], supermarketIds: [] }

      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
    })

    await page.goto('/checkout')
    await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible()
    const placeOrder = page.getByRole('button', { name: 'Pay & place order' })
    await placeOrder.click()

    await expect(page.getByRole('alert')).toContainText('Payment cancelled')
    await expect(placeOrder).toBeEnabled()
    await expect(page).toHaveURL(/\/checkout$/)
  })

  test('registers and verifies a new customer account', async ({ page }) => {
    let emailVerified = false
    let submittedCode: string | undefined

    await page.route('**/api/**', async (route) => {
      const request = route.request()
      const path = new URL(request.url()).pathname
      if (!path.startsWith('/api/')) {
        await route.continue()
        return
      }

      let body: unknown = {}
      if (path === '/api/auth/register') {
        body = { accessToken: 'new-customer-token', refreshToken: 'new-customer-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 }
      } else if (path === '/api/auth/me') {
        body = { id: 43, email: 'new.shopper@example.com', roles: ['CUSTOMER'], emailVerified }
      } else if (path === '/api/auth/verify-email') {
        submittedCode = (request.postDataJSON() as { code: string }).code
        emailVerified = true
      } else if (path === '/api/favorites') {
        body = { productIds: [], supermarketIds: [] }
      }

      await route.fulfill({ status: path === '/api/auth/verify-email' ? 204 : 200, contentType: 'application/json', body: path === '/api/auth/verify-email' ? '' : JSON.stringify(body) })
    })

    await page.goto('/register')
    await page.getByPlaceholder('Full name').fill('New Shopper')
    await page.getByPlaceholder('Email').fill('new.shopper@example.com')
    await page.getByPlaceholder('Password').fill('safe-test-password')
    await page.getByPlaceholder('Phone number').fill('+919876543210')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page).toHaveURL(/\/$/)
    await page.getByLabel('Account menu').click()
    await page.getByRole('link', { name: 'Verify your email' }).click()
    await expect(page.getByRole('heading', { name: 'Verify your email' })).toBeVisible()

    await page.getByLabel('Verification code').fill('123456')
    await page.getByRole('button', { name: 'Verify email' }).click()
    await expect(page.getByRole('heading', { name: 'Your email is verified' })).toBeVisible()
    expect(submittedCode).toBe('123456')
  })

  test('registers a supermarket and completes admin approval', async ({ page }) => {
    let supermarketVerified = false

    await page.route('**/api/**', async (route) => {
      const request = route.request()
      const path = new URL(request.url()).pathname
      if (!path.startsWith('/api/')) {
        await route.continue()
        return
      }

      const authorization = request.headers().authorization ?? ''
      let body: unknown = {}
      let status = 200

      if (path === '/api/auth/register-supermarket-owner') {
        body = {
          auth: { accessToken: 'owner-token', refreshToken: 'owner-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 },
          supermarketId: 81,
          supermarketStatus: 'PENDING',
        }
      } else if (path === '/api/auth/login') {
        const email = (request.postDataJSON() as { email: string }).email
        const admin = email === 'admin@example.com'
        body = { accessToken: admin ? 'admin-token' : 'owner-token', refreshToken: 'test-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 }
      } else if (path === '/api/auth/me') {
        body = authorization.includes('admin-token')
          ? { id: 1, email: 'admin@example.com', roles: ['ADMIN'], emailVerified: true }
          : { id: 81, email: 'owner@example.com', roles: ['SUPERMARKET_OWNER'], emailVerified: true }
      } else if (path === '/api/supermarkets/mine') {
        body = { id: 81, name: 'Green Basket Market', status: supermarketVerified ? 'VERIFIED' : 'PENDING', rejectionReason: null }
      } else if (path === '/api/admin/supermarkets') {
        body = supermarketVerified ? [] : [{ id: 81, name: 'Green Basket Market', description: 'Fresh local groceries', phone: '+919876543211', status: 'PENDING', ownerEmail: 'owner@example.com', ownerFullName: 'Store Owner' }]
      } else if (path === '/api/admin/supermarkets/81/verify') {
        supermarketVerified = true
        status = 204
      }

      await route.fulfill({
        status,
        contentType: 'application/json',
        body: status === 204 ? '' : JSON.stringify(body),
      })
    })

    await page.goto('/register-store')
    await page.getByPlaceholder('Full name').fill('Store Owner')
    await page.getByPlaceholder('Email').fill('owner@example.com')
    await page.getByPlaceholder('Password').fill('safe-owner-password')
    await page.getByPlaceholder('Phone number').fill('+919876543210')
    await page.getByPlaceholder('Supermarket name').fill('Green Basket Market')
    await page.getByPlaceholder('Short description').fill('Fresh local groceries')
    await page.getByPlaceholder('Supermarket phone').fill('+919876543211')
    await page.getByRole('button', { name: 'Submit for review' }).click()

    await expect(page.getByRole('heading', { name: 'Green Basket Market' })).toBeVisible()
    await expect(page.getByText('Pending review')).toBeVisible()
    await page.getByLabel('Account menu').click()
    await page.getByRole('button', { name: 'Log out' }).click()

    await page.goto('/login')
    await page.getByLabel('Email').fill('admin@example.com')
    await page.getByLabel('Password').fill('safe-admin-password')
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page.getByRole('heading', { name: 'Pending supermarkets' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Green Basket Market' })).toBeVisible()
    await page.getByRole('button', { name: 'Verify' }).click()
    await expect(page.getByRole('status')).toContainText('Green Basket Market verified.')

    await page.getByLabel('Account menu').click()
    await page.getByRole('button', { name: 'Log out' }).click()
    await page.goto('/login')
    await page.getByLabel('Email').fill('owner@example.com')
    await page.getByLabel('Password').fill('safe-owner-password')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByRole('heading', { name: 'Green Basket Market' })).toBeVisible()
    await expect(page.getByText('Verified', { exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: 'View orders' })).toBeVisible()
  })

  test('verifies a delivery partner and completes an assigned delivery', async ({ page }) => {
    let partnerVerified = false
    let available = false
    let deliveryStatus: 'READY_FOR_PICKUP' | 'DELIVERY_PARTNER_ASSIGNED' | 'PICKED_UP' | 'OUT_FOR_DELIVERY' | 'DELIVERED' = 'READY_FOR_PICKUP'
    let accepted = false

    const offer = () => ({
      orderId: 7001,
      status: deliveryStatus,
      supermarketName: 'Green Basket Market',
      branchName: 'Central Branch',
      pickupAddress: '12 Market Road',
      deliveryAddress: '44 Lake View Road',
      fulfilmentType: 'IMMEDIATE',
      scheduledFor: null,
      itemCount: 3,
      orderTotal: 425,
      currency: 'INR',
    })

    await page.route('**/api/**', async (route) => {
      const request = route.request()
      const path = new URL(request.url()).pathname
      if (!path.startsWith('/api/')) {
        await route.continue()
        return
      }

      const authorization = request.headers().authorization ?? ''
      let body: unknown = {}
      let status = 200

      if (path === '/api/auth/register-delivery-partner') {
        body = { accessToken: 'partner-token', refreshToken: 'partner-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 }
      } else if (path === '/api/auth/login') {
        const email = (request.postDataJSON() as { email: string }).email
        body = { accessToken: email === 'admin@example.com' ? 'admin-token' : 'partner-token', refreshToken: 'test-refresh', tokenType: 'Bearer', expiresInMillis: 3600000 }
      } else if (path === '/api/auth/me') {
        body = authorization.includes('admin-token')
          ? { id: 1, email: 'admin@example.com', roles: ['ADMIN'], emailVerified: true }
          : { id: 91, email: 'rider@example.com', roles: ['DELIVERY_PARTNER'], emailVerified: true }
      } else if (path === '/api/delivery-partner/me') {
        body = { id: 91, fullName: 'Test Rider', phone: '+919876543212', available, status: partnerVerified ? 'VERIFIED' : 'PENDING', rejectionReason: null }
      } else if (path === '/api/admin/supermarkets') {
        body = []
      } else if (path === '/api/admin/delivery-partners') {
        body = partnerVerified ? [] : [{ id: 91, fullName: 'Test Rider', email: 'rider@example.com', phone: '+919876543212', status: 'PENDING', registeredAt: '2026-07-15T08:00:00Z' }]
      } else if (path === '/api/admin/delivery-partners/91/verify') {
        partnerVerified = true
        status = 204
      } else if (path === '/api/delivery-partner/availability') {
        available = (request.postDataJSON() as { available: boolean }).available
        body = { id: 91, fullName: 'Test Rider', phone: '+919876543212', available, status: 'VERIFIED', rejectionReason: null }
      } else if (path === '/api/delivery-partner/offers') {
        body = available && !accepted && deliveryStatus === 'READY_FOR_PICKUP' ? [offer()] : []
      } else if (path === '/api/delivery-partner/active') {
        body = accepted && deliveryStatus !== 'DELIVERED' ? offer() : null
      } else if (path === '/api/delivery-partner/offers/7001/accept') {
        accepted = true
        available = false
        deliveryStatus = 'DELIVERY_PARTNER_ASSIGNED'
        body = offer()
      } else if (path === '/api/delivery-partner/deliveries/7001/pickup/verify') {
        const code = (request.postDataJSON() as { code: string }).code
        if (code !== '111111') {
          status = 400
          body = { message: 'Incorrect or expired pickup code.' }
        } else {
          deliveryStatus = 'PICKED_UP'
          body = offer()
        }
      } else if (path === '/api/delivery-partner/deliveries/7001/start') {
        deliveryStatus = 'OUT_FOR_DELIVERY'
        body = offer()
      } else if (path === '/api/delivery-partner/deliveries/7001/delivery/verify') {
        deliveryStatus = 'DELIVERED'
        accepted = false
        available = true
        body = offer()
      } else if (path === '/api/delivery-partner/earnings') {
        body = { today: deliveryStatus === 'DELIVERED' ? 45 : 0, total: deliveryStatus === 'DELIVERED' ? 45 : 0, completedDeliveries: deliveryStatus === 'DELIVERED' ? 1 : 0, currency: 'INR' }
      } else if (path === '/api/delivery-partner/history') {
        body = deliveryStatus === 'DELIVERED' ? [{ orderId: 7001, supermarketName: 'Green Basket Market', branchName: 'Central Branch', earning: 45, currency: 'INR', deliveredAt: '2026-07-15T09:00:00Z' }] : []
      } else if (path === '/api/delivery-partner/deliveries/7001/location') {
        body = { available: true, latitude: 13.55, longitude: 78.5, updatedAt: '2026-07-15T08:55:00Z' }
      }

      await route.fulfill({ status, contentType: 'application/json', body: status === 204 ? '' : JSON.stringify(body) })
    })

    await page.goto('/register-delivery-partner')
    await page.getByPlaceholder('Full name').fill('Test Rider')
    await page.getByPlaceholder('Email').fill('rider@example.com')
    await page.getByPlaceholder('Password').fill('safe-rider-password')
    await page.getByPlaceholder('Phone number').fill('+919876543212')
    await page.getByRole('button', { name: 'Create partner account' }).click()
    await expect(page.getByText('Application awaiting admin review')).toBeVisible()

    await page.getByLabel('Account menu').click()
    await page.getByRole('button', { name: 'Log out' }).click()
    await page.goto('/login')
    await page.getByLabel('Email').fill('admin@example.com')
    await page.getByLabel('Password').fill('safe-admin-password')
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page.getByRole('heading', { name: 'Pending supermarkets', exact: true })).toBeVisible()
    await page.goto('/admin/delivery-partners')
    await expect(page.getByRole('heading', { name: 'Pending delivery partners' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Test Rider' })).toBeVisible()
    await page.getByRole('button', { name: 'Approve' }).click()
    await expect(page.getByRole('status')).toContainText('Test Rider approved.')

    await page.getByLabel('Account menu').click()
    await page.getByRole('button', { name: 'Log out' }).click()
    await page.goto('/login')
    await page.getByLabel('Email').fill('rider@example.com')
    await page.getByLabel('Password').fill('safe-rider-password')
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page.getByRole('heading', { name: 'Your deliveries' })).toBeVisible()

    await page.getByRole('button', { name: 'Go online' }).click()
    await page.getByRole('button', { name: 'Accept delivery' }).click()
    await page.getByLabel('Pickup code').fill('000000')
    await page.getByRole('button', { name: 'Verify pickup' }).click()
    await expect(page.getByRole('alert')).toContainText('The code is incorrect')
    await page.getByLabel('Pickup code').fill('111111')
    await page.getByRole('button', { name: 'Verify pickup' }).click()
    await expect(page.getByText('Pickup verified')).toBeVisible()
    await page.getByRole('button', { name: 'Start delivery' }).click()
    await page.getByLabel('Delivery code').fill('222222')
    await page.getByRole('button', { name: 'Complete delivery' }).click()

    await expect(page.getByText('Order #7001 · Green Basket Market')).toBeVisible()
    const history = page.getByRole('heading', { name: 'Delivery history' }).locator('..')
    await expect(history.getByText('INR 45.00')).toBeVisible()
  })
})
