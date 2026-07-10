package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Product;

import java.math.BigDecimal;

/**
 * A product in a cross-store category browse result (see
 * {@code StoreDiscoveryService#browseCategoryNearby}) - unlike {@link ProductResponse}, this
 * also carries which store it's from and the nearest nearby branch to fulfil it from, since
 * results here are mixed across every supermarket near the customer, not scoped to one already
 * chosen store.
 */
public record CategoryProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        String currency,
        String categoryName,
        String imageUrl,
        Long supermarketId,
        String supermarketName,
        Long branchId,
        double distanceKm
) {
    public static CategoryProductResponse from(Product product, Long branchId, double distanceKm) {
        return new CategoryProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice().getAmount(),
                product.getPrice().getCurrencyCode(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl(),
                product.getSupermarket().getId(),
                product.getSupermarket().getName(),
                branchId,
                distanceKm
        );
    }
}
