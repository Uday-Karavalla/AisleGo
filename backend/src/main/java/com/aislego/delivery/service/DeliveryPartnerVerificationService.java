package com.aislego.delivery.service;

import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.delivery.domain.DeliveryPartnerProfile;
import com.aislego.delivery.domain.DeliveryPartnerStatus;
import com.aislego.delivery.dto.AdminDeliveryPartnerResponse;
import com.aislego.delivery.repository.DeliveryPartnerProfileRepository;
import com.aislego.growth.service.UserNotificationService;
import com.aislego.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class DeliveryPartnerVerificationService {
    private final DeliveryPartnerProfileRepository repository;
    private final UserRepository userRepository;
    private final UserNotificationService notificationService;

    public DeliveryPartnerVerificationService(DeliveryPartnerProfileRepository repository,
                                               UserRepository userRepository,
                                               UserNotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AdminDeliveryPartnerResponse> list(DeliveryPartnerStatus status) {
        List<DeliveryPartnerProfile> profiles = status == null ? repository.findAll()
                : repository.findByStatusOrderByCreatedAtAsc(status);
        return profiles.stream().map(AdminDeliveryPartnerResponse::from).toList();
    }

    public void verify(Long profileId, Long adminId) {
        DeliveryPartnerProfile profile = pending(profileId);
        profile.setStatus(DeliveryPartnerStatus.VERIFIED);
        profile.setReviewedAt(Instant.now());
        profile.setReviewedBy(userRepository.getReferenceById(adminId));
        repository.save(profile);
        notificationService.create(profile.getUser().getId(), "Delivery account approved",
                "Your delivery-partner account is approved. You can now go online and accept offers.", "/deliveries");
    }

    public void reject(Long profileId, Long adminId, String reason) {
        DeliveryPartnerProfile profile = pending(profileId);
        profile.setStatus(DeliveryPartnerStatus.REJECTED);
        profile.setRejectionReason(reason);
        profile.setReviewedAt(Instant.now());
        profile.setReviewedBy(userRepository.getReferenceById(adminId));
        profile.setAvailable(false);
        repository.save(profile);
        notificationService.create(profile.getUser().getId(), "Delivery application rejected",
                "Your delivery-partner application was rejected: " + reason, "/deliveries");
    }

    private DeliveryPartnerProfile pending(Long id) {
        DeliveryPartnerProfile profile = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery partner was not found"));
        if (profile.getStatus() != DeliveryPartnerStatus.PENDING) {
            throw new ConflictException("NOT_PENDING", "This delivery partner has already been reviewed");
        }
        return profile;
    }
}
