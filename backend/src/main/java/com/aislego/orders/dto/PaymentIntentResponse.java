package com.aislego.orders.dto;

/** Orders-layer projection of {@code com.aislego.payments.PaymentIntent} - kept separate so
 * the orders package doesn't leak a payments-domain type directly into its API responses. */
public record PaymentIntentResponse(
        String provider,
        boolean requiresClientAction,
        String gatewayOrderId,
        String providerKeyId,
        long amountMinorUnits,
        String currency
) {
}
