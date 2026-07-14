package com.aislego.orders.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.delivery.dto.DeliveryLocationResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public java.util.List<OrderResponse> listMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getMyOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));
    }

    public OrderStatus getMyOrderStatus(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(Order::getStatus)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));
    }

    public DeliveryLocationResponse getMyDeliveryLocation(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY || order.getDeliveryPartner() == null) {
            return DeliveryLocationResponse.unavailable();
        }
        var profile = order.getDeliveryPartner();
        if (profile.getLastLatitude() == null || profile.getLastLongitude() == null
                || profile.getLocationUpdatedAt() == null
                || profile.getLocationUpdatedAt().isBefore(Instant.now().minus(2, ChronoUnit.MINUTES))) {
            return DeliveryLocationResponse.unavailable();
        }
        return new DeliveryLocationResponse(true, profile.getLastLatitude(), profile.getLastLongitude(),
                profile.getLocationUpdatedAt());
    }
}
