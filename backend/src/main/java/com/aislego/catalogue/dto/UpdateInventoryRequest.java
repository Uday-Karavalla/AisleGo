package com.aislego.catalogue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
        @NotNull Long branchId,
        @Min(0) int quantityOnHand
) {
}
