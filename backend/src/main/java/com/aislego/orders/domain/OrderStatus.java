package com.aislego.orders.domain;

/**
 * The full order lifecycle from the product spec. Declaration order matters: tests assert
 * on {@code ordinal()} to guard against accidental reordering, which would silently break
 * any "is this status before/after that one" comparison. Only PLACED -> PAYMENT_CONFIRMED
 * is actually driven by code today (via the mock payment gateway); the rest are modelled
 * so the schema/API are stable as store-ops and delivery phases come online.
 */
public enum OrderStatus {
    PLACED,
    PAYMENT_CONFIRMED,
    ACCEPTED_BY_STORE,
    PICKING,
    SUBSTITUTION_APPROVAL,
    PACKING,
    READY_FOR_PICKUP,
    DELIVERY_PARTNER_ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
