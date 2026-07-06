package com.aislego.orders.service;

import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.AdminOrderResponse;
import com.aislego.orders.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;

    public AdminOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Every order on the platform, newest first, optionally narrowed to one status - the
     * admin-facing "what's actually happening" view, as opposed to {@link OrderService},
     * which only ever shows a customer their own orders.
     */
    public Page<AdminOrderResponse> listAll(OrderStatus statusFilter, Pageable pageable) {
        Page<com.aislego.orders.domain.Order> orders = statusFilter == null
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(statusFilter, pageable);
        return orders.map(AdminOrderResponse::from);
    }
}
