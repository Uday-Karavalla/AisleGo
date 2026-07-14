package com.aislego.delivery.service;

import com.aislego.common.exception.ConflictException;
import com.aislego.delivery.domain.DeliveryPartnerProfile;
import com.aislego.delivery.domain.DeliveryPartnerStatus;
import com.aislego.delivery.repository.DeliveryPartnerProfileRepository;
import com.aislego.growth.service.UserNotificationService;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerVerificationServiceTest {
    @Mock DeliveryPartnerProfileRepository repository;
    @Mock UserRepository userRepository;
    @Mock UserNotificationService notificationService;

    @Test
    void verifyApprovesAPendingPartnerAndNotifiesThem() {
        DeliveryPartnerProfile profile = pendingProfile();
        when(repository.findById(3L)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        DeliveryPartnerVerificationService service = new DeliveryPartnerVerificationService(
                repository, userRepository, notificationService);

        service.verify(3L, 1L);

        assertThat(profile.getStatus()).isEqualTo(DeliveryPartnerStatus.VERIFIED);
        assertThat(profile.getReviewedAt()).isNotNull();
        verify(notificationService).create(9L, "Delivery account approved",
                "Your delivery-partner account is approved. You can now go online and accept offers.", "/deliveries");
    }

    @Test
    void reviewedPartnerCannotBeReviewedAgain() {
        DeliveryPartnerProfile profile = pendingProfile();
        profile.setStatus(DeliveryPartnerStatus.REJECTED);
        when(repository.findById(3L)).thenReturn(Optional.of(profile));
        DeliveryPartnerVerificationService service = new DeliveryPartnerVerificationService(
                repository, userRepository, notificationService);

        assertThatThrownBy(() -> service.verify(3L, 1L)).isInstanceOf(ConflictException.class);
    }

    private DeliveryPartnerProfile pendingProfile() {
        User user = new User();
        user.setId(9L);
        DeliveryPartnerProfile profile = new DeliveryPartnerProfile();
        profile.setId(3L);
        profile.setUser(user);
        profile.setStatus(DeliveryPartnerStatus.PENDING);
        return profile;
    }
}
