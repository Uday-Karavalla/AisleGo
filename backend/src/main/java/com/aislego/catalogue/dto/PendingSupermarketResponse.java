package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.identity.domain.User;

/**
 * {@code ownerEmail}/{@code ownerFullName} are null for a grandfathered pre-existing
 * supermarket that has no {@code owner} (see V3 migration) rather than throwing.
 */
public record PendingSupermarketResponse(
        Long id,
        String name,
        String description,
        String phone,
        String ownerEmail,
        String ownerFullName
) {
    public static PendingSupermarketResponse from(Supermarket supermarket) {
        User owner = supermarket.getOwner();
        return new PendingSupermarketResponse(
                supermarket.getId(),
                supermarket.getName(),
                supermarket.getDescription(),
                supermarket.getPhone(),
                owner != null ? owner.getEmail() : null,
                owner != null ? owner.getFullName() : null
        );
    }
}
