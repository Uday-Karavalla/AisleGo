import { LegalLayout } from '../../components/LegalLayout'

export default function PrivacyPolicy() {
  return (
    <LegalLayout title="Privacy Policy" lastUpdated="13 July 2026">
      <p>
        This explains what information AisleGo collects when you use the app, why, and who it's shared with. It
        applies to customers and to supermarket owners who register a store.
      </p>

      <h2>1. What we collect</h2>
      <ul>
        <li><strong>Account details</strong> — your name, email address, phone number, and password (stored securely, never in plain text).</li>
        <li><strong>Delivery addresses</strong> — the addresses you save for delivery, including their location coordinates.</li>
        <li><strong>Location</strong> — your approximate location, if you allow it in your browser, so we can show you nearby stores.</li>
        <li><strong>Orders</strong> — what you order, from which store, and your order history.</li>
        <li><strong>Store details</strong> — for supermarket owners, the store name, address, contact number, and the products/stock you list.</li>
        <li><strong>Usage events</strong> — pages viewed and shopping actions such as searches, adding to cart and beginning checkout, linked to a temporary browser session and to your account when signed in.</li>
        <li><strong>Preferences</strong> — stores and products you save, referral activity, coupons and notification read status.</li>
        <li>We do not collect or store your card, UPI ID, or bank account details — those go directly to our payment partner, Razorpay.</li>
      </ul>

      <h2>2. Why we collect it</h2>
      <ul>
        <li>To create and secure your account, and to verify it's a real email and phone number.</li>
        <li>To show you supermarkets near you and let you place and track orders.</li>
        <li>To process payments and send you order updates (placed, payment confirmed, delivered, etc.).</li>
        <li>To let a store you ordered from fulfil that order — they see your name, delivery address, and order contents, not your full account details.</li>
        <li>To improve the checkout funnel, operate referral rewards and notify you when a saved product drops in price or returns to stock.</li>
      </ul>

      <h2>3. Who we share it with</h2>
      <p>We share the minimum needed with a small number of service providers who help run AisleGo:</p>
      <ul>
        <li><strong>Razorpay</strong> — to process your payment. We only receive confirmation that payment succeeded or failed, not your payment details.</li>
        <li><strong>Our email delivery provider</strong> — to send verification codes and order/account emails.</li>
        <li><strong>An SMS provider</strong>, where a store has SMS notifications enabled — to send order updates by text.</li>
        <li>We don't sell your data to anyone, and we don't share it with other stores beyond what's needed to fulfil your specific order with them.</li>
      </ul>

      <h2>4. How long we keep it</h2>
      <p>
        We keep your account and order history for as long as your account is active, so you can see your past
        orders and re-order easily. If you'd like your account or personal data deleted, contact us and we'll take
        care of it, subject to any records we're required to keep for accounting or legal reasons.
      </p>

      <h2>5. Your choices</h2>
      <ul>
        <li>You can edit your account details and delivery addresses in the app at any time.</li>
        <li>Location access is optional — you can deny or revoke it in your browser and still browse stores manually.</li>
        <li>Saving products and sharing referral links are optional. You can remove saved products at any time.</li>
        <li>To request a copy or deletion of your data, email us using the contact below.</li>
      </ul>

      <h2>Contact</h2>
      <p>
        Questions about your data? Reach us at{' '}
        <a href="mailto:AisleGo@gmail.com" className="font-semibold text-brand-700">
          AisleGo@gmail.com
        </a>
        .
      </p>
    </LegalLayout>
  )
}
