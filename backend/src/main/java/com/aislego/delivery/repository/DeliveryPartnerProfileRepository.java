package com.aislego.delivery.repository;

import com.aislego.delivery.domain.DeliveryPartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import com.aislego.delivery.domain.DeliveryPartnerStatus;

public interface DeliveryPartnerProfileRepository extends JpaRepository<DeliveryPartnerProfile, Long> {
    Optional<DeliveryPartnerProfile> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryPartnerProfile> findForUpdateByUserId(Long userId);

    List<DeliveryPartnerProfile> findByStatusOrderByCreatedAtAsc(DeliveryPartnerStatus status);
}
