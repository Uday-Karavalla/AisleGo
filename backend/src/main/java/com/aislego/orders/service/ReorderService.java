package com.aislego.orders.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.orders.domain.Order;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.dto.CartResponse;
import com.aislego.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReorderService {
    private final OrderRepository orderRepository;
    private final CartService cartService;

    public ReorderService(OrderRepository orderRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    @Transactional
    public CartResponse reorder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));
        CartResponse response = cartService.clearCart(userId);
        for (var item : order.getItems()) {
            response = cartService.addItem(userId, new AddCartItemRequest(item.getProduct().getId(), item.getQuantity()));
        }
        return response;
    }
}
