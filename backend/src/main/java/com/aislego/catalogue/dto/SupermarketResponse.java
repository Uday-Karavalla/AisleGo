package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Supermarket;

import java.util.List;

public record SupermarketResponse(
        Long id,
        String name,
        String description,
        String phone,
        String logoUrl,
        List<BranchResponse> branches
) {
    public static SupermarketResponse from(Supermarket supermarket, List<BranchResponse> branches) {
        return new SupermarketResponse(
                supermarket.getId(),
                supermarket.getName(),
                supermarket.getDescription(),
                supermarket.getPhone(),
                supermarket.getLogoUrl(),
                branches
        );
    }
}
