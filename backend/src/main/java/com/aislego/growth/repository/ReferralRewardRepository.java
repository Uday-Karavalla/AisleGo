package com.aislego.growth.repository;

import com.aislego.growth.domain.ReferralReward;
import com.aislego.growth.domain.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralRewardRepository extends JpaRepository<ReferralReward, Long> {
    Optional<ReferralReward> findByReferredUserId(Long userId);
    long countByReferrerId(Long userId);
    long countByReferrerIdAndStatus(Long userId, ReferralStatus status);
}
