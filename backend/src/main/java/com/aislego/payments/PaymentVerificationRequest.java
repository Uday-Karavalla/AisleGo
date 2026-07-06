package com.aislego.payments;

/**
 * What the client (or a gateway webhook) reports back after completing payment. Fields are
 * gateway-specific and may be {@code null} for providers that don't need them - Mock ignores
 * all three.
 */
public record PaymentVerificationRequest(
        String gatewayOrderId,
        String gatewayPaymentId,
        String gatewaySignature
) {
}
