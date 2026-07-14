package com.aislego.delivery.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.delivery.domain.DeliveryPartnerProfile;
import com.aislego.delivery.domain.DeliveryPartnerStatus;
import com.aislego.delivery.dto.DeliveryPartnerProfileResponse;
import com.aislego.delivery.dto.DeliveryOfferResponse;
import com.aislego.delivery.dto.DeliveryLocationResponse;
import com.aislego.delivery.dto.DeliveryHistoryResponse;
import com.aislego.delivery.dto.DeliveryEarningsResponse;
import com.aislego.delivery.repository.DeliveryPartnerProfileRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.orders.domain.FulfilmentType;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.repository.OrderRepository;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.aislego.growth.service.UserNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeliveryPartnerService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DeliveryPartnerProfileRepository repository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserNotificationService notificationService;

    public DeliveryPartnerService(DeliveryPartnerProfileRepository repository, OrderRepository orderRepository,
                                  PasswordEncoder passwordEncoder, UserNotificationService notificationService) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOfferResponse> listOffers(Long userId) {
        DeliveryPartnerProfile profile = findByUserId(userId);
        if (!profile.isAvailable()) return List.of();
        return orderRepository.findByStatusAndFulfilmentTypeNotOrderByCreatedAtAsc(
                        OrderStatus.READY_FOR_PICKUP, FulfilmentType.PICKUP)
                .stream().map(DeliveryOfferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryOfferResponse getActiveDelivery(Long userId) {
        return orderRepository.findFirstByDeliveryPartnerUserIdAndStatusInOrderByCreatedAtDesc(userId,
                        Set.of(OrderStatus.DELIVERY_PARTNER_ASSIGNED, OrderStatus.PICKED_UP,
                                OrderStatus.OUT_FOR_DELIVERY))
                .map(DeliveryOfferResponse::from).orElse(null);
    }

    public DeliveryOfferResponse acceptOffer(Long userId, Long orderId) {
        DeliveryPartnerProfile profile = repository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Delivery partner profile was not found"));
        if (!profile.isAvailable()) {
            throw new ConflictException("PARTNER_OFFLINE", "Go online before accepting a delivery");
        }
        Order order = orderRepository.findForUpdateById(orderId)
                .orElseThrow(() -> new NotFoundException("Delivery offer was not found"));
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP
                || order.getFulfilmentType() == FulfilmentType.PICKUP
                || order.getDeliveryPartner() != null) {
            throw new ConflictException("OFFER_UNAVAILABLE", "This delivery offer is no longer available");
        }
        order.setDeliveryPartner(profile);
        order.setStatus(OrderStatus.DELIVERY_PARTNER_ASSIGNED);
        String pickupCode = newOtp();
        String deliveryCode = newOtp();
        order.setPickupOtpHash(passwordEncoder.encode(pickupCode));
        order.setDeliveryOtpHash(passwordEncoder.encode(deliveryCode));
        profile.setAvailable(false);
        repository.save(profile);
        orderRepository.save(order);
        if (order.getSupermarket().getOwner() != null) {
            notificationService.create(order.getSupermarket().getOwner().getId(), "Pickup code for order #" + order.getId(),
                    "Give pickup code " + pickupCode + " to the assigned delivery partner after handing over the order.",
                    "/my-store/orders");
        }
        notificationService.create(order.getUser().getId(), "Delivery code for order #" + order.getId(),
                "Give delivery code " + deliveryCode + " to the partner only after receiving your order.",
                "/orders/" + order.getId());
        return DeliveryOfferResponse.from(order);
    }

    public DeliveryOfferResponse verifyPickup(Long userId, Long orderId, String code) {
        Order order = assignedOrderForUpdate(userId, orderId);
        if (order.getStatus() != OrderStatus.DELIVERY_PARTNER_ASSIGNED) {
            throw new ConflictException("INVALID_DELIVERY_STAGE", "This order is not awaiting pickup verification");
        }
        verifyCode(order.getPickupOtpHash(), code);
        order.setPickupOtpHash(null);
        order.setStatus(OrderStatus.PICKED_UP);
        return DeliveryOfferResponse.from(orderRepository.save(order));
    }

    public DeliveryOfferResponse startDelivery(Long userId, Long orderId) {
        Order order = assignedOrderForUpdate(userId, orderId);
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new ConflictException("INVALID_DELIVERY_STAGE", "Verify pickup before starting delivery");
        }
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        return DeliveryOfferResponse.from(orderRepository.save(order));
    }

    public DeliveryOfferResponse verifyDelivery(Long userId, Long orderId, String code) {
        Order order = assignedOrderForUpdate(userId, orderId);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new ConflictException("INVALID_DELIVERY_STAGE", "Start delivery before completing it");
        }
        verifyCode(order.getDeliveryOtpHash(), code);
        order.setDeliveryOtpHash(null);
        order.setStatus(OrderStatus.DELIVERED);
        DeliveryPartnerProfile profile = order.getDeliveryPartner();
        profile.setAvailable(true);
        clearLocation(profile);
        repository.save(profile);
        return DeliveryOfferResponse.from(orderRepository.save(order));
    }

    public DeliveryLocationResponse updateLocation(Long userId, Long orderId, double latitude, double longitude) {
        Order order = assignedOrderForUpdate(userId, orderId);
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new ConflictException("LOCATION_SHARING_INACTIVE", "Location can only be shared while out for delivery");
        }
        DeliveryPartnerProfile profile = order.getDeliveryPartner();
        profile.setLastLatitude(latitude);
        profile.setLastLongitude(longitude);
        profile.setLocationUpdatedAt(Instant.now());
        repository.save(profile);
        return new DeliveryLocationResponse(true, latitude, longitude, profile.getLocationUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<DeliveryHistoryResponse> history(Long userId) {
        findByUserId(userId);
        return completedOrders(userId).stream().map(DeliveryHistoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryEarningsResponse earnings(Long userId) {
        findByUserId(userId);
        List<Order> completed = completedOrders(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        BigDecimal total = completed.stream().map(Order::getDeliveryFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayTotal = completed.stream()
                .filter(order -> order.getUpdatedAt().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().equals(today))
                .map(Order::getDeliveryFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = completed.isEmpty() ? "INR" : completed.get(0).getTotalAmount().getCurrencyCode();
        return new DeliveryEarningsResponse(todayTotal, total, completed.size(), currency);
    }

    private List<Order> completedOrders(Long userId) {
        return orderRepository.findByDeliveryPartnerUserIdAndStatusOrderByUpdatedAtDesc(userId, OrderStatus.DELIVERED);
    }

    private void clearLocation(DeliveryPartnerProfile profile) {
        profile.setLastLatitude(null);
        profile.setLastLongitude(null);
        profile.setLocationUpdatedAt(null);
    }

    private Order assignedOrderForUpdate(Long userId, Long orderId) {
        Order order = orderRepository.findForUpdateById(orderId)
                .orElseThrow(() -> new NotFoundException("Delivery was not found"));
        if (order.getDeliveryPartner() == null || !order.getDeliveryPartner().getUser().getId().equals(userId)) {
            throw new NotFoundException("Delivery was not found");
        }
        return order;
    }

    private void verifyCode(String hash, String code) {
        if (hash == null || !passwordEncoder.matches(code, hash)) {
            throw new ConflictException("INVALID_OTP", "The code is incorrect");
        }
    }

    private String newOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    @Transactional(readOnly = true)
    public DeliveryPartnerProfileResponse getProfile(Long userId) {
        return DeliveryPartnerProfileResponse.from(findByUserId(userId));
    }

    public DeliveryPartnerProfileResponse updateAvailability(Long userId, boolean available) {
        DeliveryPartnerProfile profile = findByUserId(userId);
        if (available && profile.getStatus() != DeliveryPartnerStatus.VERIFIED) {
            throw new ConflictException("PARTNER_NOT_VERIFIED", "Your account must be approved before you can go online");
        }
        profile.setAvailable(available);
        return DeliveryPartnerProfileResponse.from(repository.save(profile));
    }

    private DeliveryPartnerProfile findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Delivery partner profile was not found"));
    }
}
