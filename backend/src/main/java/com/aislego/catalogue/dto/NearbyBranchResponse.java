package com.aislego.catalogue.dto;

import com.aislego.catalogue.repository.NearbyBranchView;
import com.aislego.catalogue.routing.RouteEstimate;

/**
 * {@code distanceKm}/{@code etaMinutes}: real driving distance/ETA when ORS is enabled
 * ({@code aislego.routing.provider=openrouteservice}), great-circle estimate otherwise - see
 * {@code RoutingService}.
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
        boolean isOpen
) {
    public static NearbyBranchResponse from(NearbyBranchView view, RouteEstimate estimate, boolean isOpen) {
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
                isOpen
        );
    }
}
