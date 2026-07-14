package com.aislego.delivery.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(@NotNull Boolean available) {
}
