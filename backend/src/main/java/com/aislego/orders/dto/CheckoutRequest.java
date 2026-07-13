package com.aislego.orders.dto;

import jakarta.validation.constraints.NotNull;

import com.aislego.orders.domain.FulfilmentType;

import java.time.Instant;

/** Branch and fulfilment choice for this checkout. Delivery requires an address owned by
 *  the customer; scheduled delivery additionally requires a future timestamp. Pickup uses
 *  neither an address nor a scheduled timestamp. */
public record CheckoutRequest(
        @NotNull Long branchId,
        @NotNull FulfilmentType fulfilmentType,
        Long addressId,
        Instant scheduledFor
) {
}
