package com.aislego.delivery.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.common.exception.ConflictException;
import com.aislego.delivery.domain.DeliveryPartnerProfile;
import com.aislego.delivery.domain.DeliveryPartnerStatus;
import com.aislego.delivery.dto.DeliveryPartnerProfileResponse;
import com.aislego.delivery.repository.DeliveryPartnerProfileRepository;
import com.aislego.identity.domain.User;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.growth.service.UserNotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerServiceTest {
    @Mock
    private DeliveryPartnerProfileRepository repository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserNotificationService notificationService;

    @Test
    void updateAvailabilityPersistsThePartnersNewState() {
        DeliveryPartnerProfile profile = profile();
        when(repository.findByUserId(9L)).thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        DeliveryPartnerProfileResponse response = service.updateAvailability(9L, true);

        assertThat(response.available()).isTrue();
        verify(repository).save(profile);
    }

    @Test
    void getProfileDoesNotExposeAnotherOrMissingPartner() {
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        assertThatThrownBy(() -> service.getProfile(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void pickupVerificationRejectsAnIncorrectCode() {
        DeliveryPartnerProfile profile = profile();
        Order order = new Order();
        order.setId(44L);
        order.setDeliveryPartner(profile);
        order.setStatus(OrderStatus.DELIVERY_PARTNER_ASSIGNED);
        order.setPickupOtpHash("hashed-code");
        when(orderRepository.findForUpdateById(44L)).thenReturn(Optional.of(order));
        when(passwordEncoder.matches("111111", "hashed-code")).thenReturn(false);
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        assertThatThrownBy(() -> service.verifyPickup(9L, 44L, "111111"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("incorrect");
    }

    @Test
    void pickupVerificationRejectsAReusedCodeAfterTheStageAdvanced() {
        Order order = new Order();
        order.setId(44L);
        order.setDeliveryPartner(profile());
        order.setStatus(OrderStatus.PICKED_UP);
        when(orderRepository.findForUpdateById(44L)).thenReturn(Optional.of(order));
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        assertThatThrownBy(() -> service.verifyPickup(9L, 44L, "123456"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not awaiting pickup");
    }

    @Test
    void liveLocationCanOnlyBeUpdatedForTheAssignedOutForDeliveryOrder() {
        Order order = new Order();
        order.setId(44L);
        order.setDeliveryPartner(profile());
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        when(orderRepository.findForUpdateById(44L)).thenReturn(Optional.of(order));
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        var location = service.updateLocation(9L, 44L, 13.55, 78.50);

        assertThat(location.available()).isTrue();
        assertThat(location.latitude()).isEqualTo(13.55);
        assertThat(order.getDeliveryPartner().getLocationUpdatedAt()).isNotNull();
        verify(repository).save(order.getDeliveryPartner());
    }

    @Test
    void liveLocationIsRejectedBeforeThePartnerStartsDelivery() {
        Order order = new Order();
        order.setId(44L);
        order.setDeliveryPartner(profile());
        order.setStatus(OrderStatus.PICKED_UP);
        when(orderRepository.findForUpdateById(44L)).thenReturn(Optional.of(order));
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        assertThatThrownBy(() -> service.updateLocation(9L, 44L, 13.55, 78.50))
                .isInstanceOf(ConflictException.class).hasMessageContaining("only be shared");
    }

    @Test
    void earningsUseSnapshottedDeliveryFeesFromCompletedDeliveries() {
        DeliveryPartnerProfile profile = profile();
        when(repository.findByUserId(9L)).thenReturn(Optional.of(profile));
        Order first = deliveredOrder(BigDecimal.valueOf(25));
        Order second = deliveredOrder(BigDecimal.valueOf(40));
        when(orderRepository.findByDeliveryPartnerUserIdAndStatusOrderByUpdatedAtDesc(9L, OrderStatus.DELIVERED))
                .thenReturn(List.of(first, second));
        DeliveryPartnerService service = new DeliveryPartnerService(repository, orderRepository, passwordEncoder, notificationService);

        var earnings = service.earnings(9L);

        assertThat(earnings.total()).isEqualByComparingTo("65.00");
        assertThat(earnings.completedDeliveries()).isEqualTo(2);
        assertThat(earnings.currency()).isEqualTo("INR");
    }

    private DeliveryPartnerProfile profile() {
        User user = new User();
        user.setId(9L);
        user.setFullName("Ravi Rider");
        user.setPhone("+919000000000");
        DeliveryPartnerProfile profile = new DeliveryPartnerProfile();
        profile.setId(3L);
        profile.setUser(user);
        profile.setStatus(DeliveryPartnerStatus.VERIFIED);
        return profile;
    }

    private Order deliveredOrder(BigDecimal fee) {
        Order order = new Order();
        order.setDeliveryFee(fee);
        order.setUpdatedAt(Instant.now());
        order.setTotalAmount(com.aislego.common.money.Money.of(BigDecimal.valueOf(100), "INR"));
        return order;
    }
}
