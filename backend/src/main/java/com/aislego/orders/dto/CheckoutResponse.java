package com.aislego.orders.dto;

/** Response for {@code POST /api/checkout}: the order just placed plus whatever the client
 * needs to complete payment (immediately, for Mock; via a gateway widget, for Razorpay). */
public record CheckoutResponse(
        OrderResponse order,
        PaymentIntentResponse payment
) {
}
