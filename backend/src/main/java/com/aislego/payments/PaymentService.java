package com.aislego.payments;

import com.aislego.common.money.Money;

/**
 * Abstraction over "however we charge the customer." A real gateway (Razorpay today) must
 * never let card/UPI details reach this backend (PCI scope + the "never store payment details
 * directly" system requirement), so the flow is necessarily two-phase and partly asynchronous:
 *
 * <ol>
 *     <li>{@link #createIntent} sets up a charge with the gateway (e.g. a Razorpay order) and
 *     returns whatever the client needs to complete it directly with the gateway.</li>
 *     <li>{@link #verifyAndCapture} verifies a signed result the client (or the gateway's
 *     webhook) reports back, without trusting the client's word for it.</li>
 * </ol>
 *
 * <p>{@link MockPaymentGateway} is the zero-setup default that skips the client round-trip
 * entirely (no {@code requiresClientAction}) for local/demo use. {@link RazorpayPaymentGateway}
 * is the opt-in real-gateway implementation.
 */
public interface PaymentService {

    PaymentIntent createIntent(Long orderId, Money amount);

    PaymentVerificationResult verifyAndCapture(String gatewayReference, PaymentVerificationRequest request);
}
