package com.aislego.catalogue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String sku,
        @NotNull @Positive BigDecimal price,
        @NotBlank String currency,
        String categoryName,
        String imageUrl,
        @NotNull Long branchId,
        @Min(0) int initialStockQuantity
) {
}
