package com.aislego.delivery.dto;

import java.time.Instant;

public record DeliveryLocationResponse(boolean available, Double latitude, Double longitude, Instant updatedAt) {
    public static DeliveryLocationResponse unavailable() {
        return new DeliveryLocationResponse(false, null, null, null);
    }
}
