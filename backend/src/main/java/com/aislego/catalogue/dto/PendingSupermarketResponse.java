package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.identity.domain.User;

/**
 * {@code ownerEmail}/{@code ownerFullName} are null for a grandfathered pre-existing
 * supermarket that has no {@code owner} (see V3 migration) rather than throwing. Despite the
 * name, this now also backs the admin's "all supermarkets" directory view, not just the
 * pending-review queue - {@code status} is what lets that view tell them apart.
 */
public record PendingSupermarketResponse(
        Long id,
        String name,
        String description,
        String phone,
        SupermarketStatus status,
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
                supermarket.getStatus(),
                owner != null ? owner.getEmail() : null,
                owner != null ? owner.getFullName() : null
        );
    }
}
