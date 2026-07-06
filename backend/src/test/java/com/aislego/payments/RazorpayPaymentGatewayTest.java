package com.aislego.payments;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises signature verification only - no network call, since {@code RazorpayClient}'s
 * constructor does no I/O and {@code createIntent} (the only method that would call the
 * network) is not under test here.
 */
class RazorpayPaymentGatewayTest {

    private static final String KEY_SECRET = "test_key_secret";

    @Test
    void verifyAndCaptureSucceedsForACorrectlySignedPayment() throws RazorpayException {
        RazorpayPaymentGateway gateway = new RazorpayPaymentGateway("rzp_test_dummy", KEY_SECRET);

        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String signature = Utils.getHash(orderId + "|" + paymentId, KEY_SECRET);

        PaymentVerificationRequest request = new PaymentVerificationRequest(orderId, paymentId, signature);

        PaymentVerificationResult result = gateway.verifyAndCapture(orderId, request);

        assertThat(result.success()).isTrue();
    }

    @Test
    void verifyAndCaptureFailsForATamperedSignature() throws RazorpayException {
        RazorpayPaymentGateway gateway = new RazorpayPaymentGateway("rzp_test_dummy", KEY_SECRET);

        PaymentVerificationRequest request = new PaymentVerificationRequest(
                "order_ABC123", "pay_XYZ789", "not-a-valid-signature");

        PaymentVerificationResult result = gateway.verifyAndCapture("order_ABC123", request);

        assertThat(result.success()).isFalse();
    }

    @Test
    void verifyAndCaptureFailsWhenSignedWithADifferentSecret() throws RazorpayException {
        RazorpayPaymentGateway gateway = new RazorpayPaymentGateway("rzp_test_dummy", KEY_SECRET);

        String orderId = "order_ABC123";
        String paymentId = "pay_XYZ789";
        String signature = Utils.getHash(orderId + "|" + paymentId, "a-completely-different-secret");

        PaymentVerificationRequest request = new PaymentVerificationRequest(orderId, paymentId, signature);

        PaymentVerificationResult result = gateway.verifyAndCapture(orderId, request);

        assertThat(result.success()).isFalse();
    }
}
