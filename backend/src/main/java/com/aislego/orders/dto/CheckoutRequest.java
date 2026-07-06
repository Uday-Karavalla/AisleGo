package com.aislego.orders.dto;

import jakarta.validation.constraints.NotNull;

/** Branch chosen for fulfilment - must belong to the same supermarket as the cart's items.
 *  {@code addressId}, if given, must belong to the calling customer; null for a pickup order
 *  or when the customer has no saved address yet. */
public record CheckoutRequest(
        @NotNull Long branchId,
        Long addressId
) {
}
