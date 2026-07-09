import { LegalLayout } from '../../components/LegalLayout'

export default function Terms() {
  return (
    <LegalLayout title="Terms of Service" lastUpdated="9 July 2026">
      <p>
        AisleGo is an online marketplace that connects you with independent supermarkets near you. You can browse
        their catalogues, place orders, and pay online through the app. By creating an account or placing an order
        on AisleGo, you agree to these terms.
      </p>

      <h2>1. What AisleGo is, and isn't</h2>
      <p>
        AisleGo operates the app and website. It does not own or run the supermarkets listed on it — each store is
        an independent business responsible for its own product listings, pricing, stock accuracy, and order
        fulfilment. AisleGo is the platform that connects you to them and processes payment on their behalf.
      </p>

      <h2>2. Accounts</h2>
      <ul>
        <li>You must register with a genuine email address and a real, working phone number. Fake or disposable emails are not permitted, and accounts found to use them may be suspended.</li>
        <li>You're responsible for keeping your password confidential and for all activity under your account.</li>
        <li>Supermarket owners must provide accurate business details when registering a store. Every new store is reviewed by an AisleGo admin before it becomes visible to customers.</li>
      </ul>

      <h2>3. Orders and payment</h2>
      <ul>
        <li>When you place an order, you're making an offer to buy from that specific store at the listed price. The order is confirmed once payment succeeds.</li>
        <li>Payments are processed through our payment partner, Razorpay. AisleGo does not store your card, UPI, or bank details.</li>
        <li>Product availability can change between browsing and checkout. If an item you ordered turns out to be unavailable, the store may substitute it (with your approval where the app supports that) or remove it from your order before it ships.</li>
      </ul>

      <h2>4. Cancellations and delivery</h2>
      <p>
        See our <a href="/legal/refunds" className="font-semibold text-brand-700">Refund &amp; Cancellation Policy</a> for
        how order cancellations and refunds are handled. Delivery times shown in the app are estimates from the
        store, not a guarantee from AisleGo.
      </p>

      <h2>5. Acceptable use</h2>
      <p>
        Don't use AisleGo to place fraudulent orders, misuse another person's account or payment details, scrape or
        resell store data, or interfere with the normal operation of the app. We can suspend or close accounts that
        do.
      </p>

      <h2>6. Liability</h2>
      <p>
        AisleGo connects you with independent stores in good faith but isn't responsible for the quality, safety, or
        legality of the products they sell — that responsibility sits with the store. Where something goes wrong
        with an order, contact the store first through the app; AisleGo will step in to help resolve disputes where
        it reasonably can.
      </p>

      <h2>7. Changes</h2>
      <p>
        We may update these terms as AisleGo grows. If we make a material change, we'll let you know in the app.
        Continuing to use AisleGo after a change means you accept the updated terms.
      </p>

      <h2>Contact</h2>
      <p>
        Questions about these terms? Reach us at{' '}
        <a href="mailto:AisleGo@gmail.com" className="font-semibold text-brand-700">
          AisleGo@gmail.com
        </a>
        .
      </p>
    </LegalLayout>
  )
}
