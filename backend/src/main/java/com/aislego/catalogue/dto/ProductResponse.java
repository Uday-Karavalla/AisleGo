package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        Long supermarketId,
        String name,
        String description,
        String sku,
        BigDecimal price,
        String currency,
        String categoryName,
        String imageUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSupermarket().getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice().getAmount(),
                product.getPrice().getCurrencyCode(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl()
        );
    }
}
