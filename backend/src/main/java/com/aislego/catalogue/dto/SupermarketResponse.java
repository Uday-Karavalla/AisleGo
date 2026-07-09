package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.reviews.repository.RatingSummaryView;

import java.util.List;

public record SupermarketResponse(
        Long id,
        String name,
        String description,
        String phone,
        String logoUrl,
        List<BranchResponse> branches,
        Double rating,
        long ratingCount
) {
    public static SupermarketResponse from(Supermarket supermarket, List<BranchResponse> branches,
                                            RatingSummaryView ratingSummary) {
        return new SupermarketResponse(
                supermarket.getId(),
                supermarket.getName(),
                supermarket.getDescription(),
                supermarket.getPhone(),
                supermarket.getLogoUrl(),
                branches,
                ratingSummary != null ? ratingSummary.getAverageRating() : null,
                ratingSummary != null ? ratingSummary.getReviewCount() : 0
        );
    }
}
