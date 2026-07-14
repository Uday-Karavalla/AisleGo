package com.aislego.delivery.dto;

import com.aislego.delivery.domain.DeliveryPartnerProfile;

public record DeliveryPartnerProfileResponse(Long id, String fullName, String phone, boolean available,
                                              String status, String rejectionReason) {
    public static DeliveryPartnerProfileResponse from(DeliveryPartnerProfile profile) {
        return new DeliveryPartnerProfileResponse(profile.getId(), profile.getUser().getFullName(),
                profile.getUser().getPhone(), profile.isAvailable(), profile.getStatus().name(),
                profile.getRejectionReason());
    }
}
