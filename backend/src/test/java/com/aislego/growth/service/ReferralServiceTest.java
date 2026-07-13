package com.aislego.growth.service;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.repository.CouponRepository;
import com.aislego.growth.domain.ReferralReward;
import com.aislego.growth.domain.ReferralStatus;
import com.aislego.growth.repository.ReferralRewardRepository;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {
    @Mock UserRepository userRepository;
    @Mock ReferralRewardRepository referralRepository;
    @Mock CouponRepository couponRepository;
    ReferralService service;

    @BeforeEach void setUp() {
        service = new ReferralService(userRepository, referralRepository, couponRepository);
        lenient().when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newCustomerReceivesPrivateSingleUseWelcomeCoupon() {
        User user = user(10L, "Maya Rao");
        when(userRepository.findByReferralCodeIgnoreCase(any())).thenReturn(Optional.empty());

        service.prepareNewCustomer(user, null);

        ArgumentCaptor<Coupon> coupon = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(coupon.capture());
        assertThat(coupon.getValue().getAssignedUser()).isSameAs(user);
        assertThat(coupon.getValue().isFirstOrderOnly()).isTrue();
        assertThat(coupon.getValue().getPerUserLimit()).isEqualTo(1);
        assertThat(coupon.getValue().getAmountOff().getAmount()).isEqualByComparingTo("100.00");
        assertThat(user.getReferralCode()).startsWith("AG");
    }

    @Test
    void firstPurchaseRewardsBothSidesExactlyOnce() {
        User referrer = user(1L, "Referrer");
        User referred = user(2L, "Friend");
        ReferralReward reward = new ReferralReward();
        reward.setReferrer(referrer);
        reward.setReferredUser(referred);
        reward.setStatus(ReferralStatus.PENDING);
        when(referralRepository.findByReferredUserId(2L)).thenReturn(Optional.of(reward));

        service.rewardAfterFirstPurchase(2L);

        verify(couponRepository, org.mockito.Mockito.times(2)).save(any(Coupon.class));
        assertThat(reward.getStatus()).isEqualTo(ReferralStatus.REWARDED);
        assertThat(reward.getRewardedAt()).isNotNull();
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        return user;
    }
}
