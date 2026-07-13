package com.aislego.growth.service;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.DiscountType;
import com.aislego.catalogue.repository.CouponRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.growth.domain.ReferralReward;
import com.aislego.growth.domain.ReferralStatus;
import com.aislego.growth.dto.ReferralSummaryResponse;
import com.aislego.growth.repository.ReferralRewardRepository;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class ReferralService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BigDecimal REWARD_AMOUNT = BigDecimal.valueOf(100);

    private final UserRepository userRepository;
    private final ReferralRewardRepository referralRepository;
    private final CouponRepository couponRepository;

    public ReferralService(UserRepository userRepository, ReferralRewardRepository referralRepository,
                           CouponRepository couponRepository) {
        this.userRepository = userRepository;
        this.referralRepository = referralRepository;
        this.couponRepository = couponRepository;
    }

    /** Called immediately after a customer is first persisted. Issues a private welcome offer
     * and records the inviter, if a valid code was supplied. */
    @Transactional
    public void prepareNewCustomer(User user, String suppliedReferralCode) {
        ensureReferralCode(user);
        issueCoupon(user, "WELCOME", true);

        if (suppliedReferralCode == null || suppliedReferralCode.isBlank()) return;
        User referrer = userRepository.findByReferralCodeIgnoreCase(suppliedReferralCode.trim())
                .orElseThrow(() -> new BadRequestException("That referral code is not valid"));
        if (referrer.getId().equals(user.getId())) {
            throw new BadRequestException("You cannot refer yourself");
        }
        user.setReferredBy(referrer);
        userRepository.save(user);

        ReferralReward referral = new ReferralReward();
        referral.setReferrer(referrer);
        referral.setReferredUser(user);
        referral.setStatus(ReferralStatus.PENDING);
        referral.setCreatedAt(Instant.now());
        referralRepository.save(referral);
    }

    /** Rewards both people only after the invited shopper's first successful payment. */
    @Transactional
    public void rewardAfterFirstPurchase(Long referredUserId) {
        referralRepository.findByReferredUserId(referredUserId).ifPresent(referral -> {
            if (referral.getStatus() == ReferralStatus.REWARDED) return;
            issueCoupon(referral.getReferrer(), "REFER", false);
            issueCoupon(referral.getReferredUser(), "THANKS", false);
            referral.setStatus(ReferralStatus.REWARDED);
            referral.setRewardedAt(Instant.now());
            referralRepository.save(referral);
        });
    }

    @Transactional
    public ReferralSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Account no longer exists"));
        ensureReferralCode(user);
        var rewardCodes = couponRepository.findByAssignedUserIdAndActiveTrueOrderByCreatedAtDesc(userId).stream()
                .map(Coupon::getCode)
                .toList();
        return new ReferralSummaryResponse(user.getReferralCode(),
                referralRepository.countByReferrerId(userId),
                referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.REWARDED),
                rewardCodes);
    }

    private void ensureReferralCode(User user) {
        if (user.getReferralCode() != null) return;
        String stem = user.getFullName().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (stem.length() > 8) stem = stem.substring(0, 8);
        String candidate;
        do {
            candidate = "AG" + stem + Integer.toString(RANDOM.nextInt(36 * 36 * 36), 36).toUpperCase(Locale.ROOT);
        } while (userRepository.findByReferralCodeIgnoreCase(candidate).isPresent());
        user.setReferralCode(candidate);
        userRepository.save(user);
    }

    private void issueCoupon(User user, String prefix, boolean firstOrderOnly) {
        Coupon coupon = new Coupon();
        coupon.setCode(prefix + "-" + user.getId() + "-" + Integer.toString(RANDOM.nextInt(36 * 36), 36)
                .toUpperCase(Locale.ROOT));
        coupon.setDiscountType(DiscountType.FLAT);
        coupon.setAmountOff(Money.of(REWARD_AMOUNT, "INR"));
        coupon.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        coupon.setActive(true);
        coupon.setFirstOrderOnly(firstOrderOnly);
        coupon.setMaxRedemptions(1);
        coupon.setPerUserLimit(1);
        coupon.setAssignedUser(user);
        couponRepository.save(coupon);
    }
}
