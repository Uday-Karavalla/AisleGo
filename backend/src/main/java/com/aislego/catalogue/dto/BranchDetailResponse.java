package com.aislego.catalogue.dto;

/**
 * A single branch's public-facing detail, resolved directly by branch id (not nested inside a
 * {@link SupermarketResponse}) - backs {@code GET /api/stores/branches/{branchId}}, which is
 * what the storefront route actually navigates by (see {@code NearbyBranchResponse}: discovery
 * results are branches, since distance/ETA/open-hours are branch-specific), separately from
 * {@code supermarketId}, which the storefront then uses for the shared product catalogue,
 * categories and reviews - those live on the supermarket, not the branch.
 */
public record BranchDetailResponse(
        Long branchId,
        String branchName,
        String addressLine,
        String city,
        boolean isOpen,
        Long supermarketId,
        String supermarketName,
        String logoUrl,
        Double rating,
        long ratingCount
) {
}
