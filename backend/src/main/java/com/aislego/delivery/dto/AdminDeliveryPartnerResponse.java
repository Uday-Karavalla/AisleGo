package com.aislego.delivery.dto;

import com.aislego.delivery.domain.DeliveryPartnerProfile;
import java.time.Instant;

public record AdminDeliveryPartnerResponse(Long id, String fullName, String email, String phone,
                                           String status, Instant registeredAt) {
    public static AdminDeliveryPartnerResponse from(DeliveryPartnerProfile profile) {
        return new AdminDeliveryPartnerResponse(profile.getId(), profile.getUser().getFullName(),
                profile.getUser().getEmail(), profile.getUser().getPhone(), profile.getStatus().name(),
                profile.getCreatedAt());
    }
}
