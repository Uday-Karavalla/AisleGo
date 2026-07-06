package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Branch;

public record BranchResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        double latitude,
        double longitude,
        String openingTime,
        String closingTime
) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddressLine(),
                branch.getCity(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getOpeningTime(),
                branch.getClosingTime()
        );
    }
}
