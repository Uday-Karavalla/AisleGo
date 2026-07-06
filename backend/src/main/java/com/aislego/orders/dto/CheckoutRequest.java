package com.aislego.orders.dto;

import jakarta.validation.constraints.NotNull;

/** Branch chosen for fulfilment - must belong to the same supermarket as the cart's items. */
public record CheckoutRequest(
        @NotNull Long branchId
) {
}
