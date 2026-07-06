package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Product;

import java.math.BigDecimal;
import java.util.List;

public record OwnerProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        String currency,
        String categoryName,
        String imageUrl,
        boolean active,
        List<BranchStockResponse> branchStock
) {
    public static OwnerProductResponse from(Product product, List<BranchStockResponse> branchStock) {
        return new OwnerProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice().getAmount(),
                product.getPrice().getCurrencyCode(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl(),
                product.isActive(),
                branchStock
        );
    }
}
