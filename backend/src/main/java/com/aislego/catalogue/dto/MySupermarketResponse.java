package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Supermarket;

public record MySupermarketResponse(
        Long id,
        String name,
        String status,
        String rejectionReason
) {
    public static MySupermarketResponse from(Supermarket supermarket) {
        return new MySupermarketResponse(
                supermarket.getId(),
                supermarket.getName(),
                supermarket.getStatus().name(),
                supermarket.getRejectionReason()
        );
    }
}
