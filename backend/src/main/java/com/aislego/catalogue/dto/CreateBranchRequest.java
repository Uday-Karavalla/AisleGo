package com.aislego.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBranchRequest(
        @NotBlank String name,
        String addressLine,
        String city,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String openingTime,
        String closingTime
) {
}
