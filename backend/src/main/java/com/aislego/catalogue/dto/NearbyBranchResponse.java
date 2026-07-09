package com.aislego.catalogue.dto;

import com.aislego.catalogue.repository.NearbyBranchView;
import com.aislego.catalogue.routing.RouteEstimate;
import com.aislego.reviews.repository.RatingSummaryView;

/**
 * {@code distanceKm}/{@code etaMinutes}: real driving distance/ETA when ORS is enabled
 * ({@code aislego.routing.provider=openrouteservice}), great-circle estimate otherwise - see
 * {@code RoutingService}. {@code rating}/{@code ratingCount} are null/0 for a store with no
 * reviews yet, not a zero-star rating - see {@code ReviewService#summarize}.
 */
public record NearbyBranchResponse(
        Long branchId,
        String branchName,
        String addressLine,
        String city,
        double latitude,
        double longitude,
        Long supermarketId,
        String supermarketName,
        double distanceKm,
        double etaMinutes,
        boolean isOpen,
        Double rating,
        long ratingCount
) {
    public static NearbyBranchResponse from(NearbyBranchView view, RouteEstimate estimate, boolean isOpen,
                                             RatingSummaryView ratingSummary) {
        return new NearbyBranchResponse(
                view.getId(),
                view.getName(),
                view.getAddressLine(),
                view.getCity(),
                view.getLatitude(),
                view.getLongitude(),
                view.getSupermarketId(),
                view.getSupermarketName(),
                estimate.distanceKm(),
                estimate.durationMinutes(),
                isOpen,
                ratingSummary != null ? ratingSummary.getAverageRating() : null,
                ratingSummary != null ? ratingSummary.getReviewCount() : 0
        );
    }
}
