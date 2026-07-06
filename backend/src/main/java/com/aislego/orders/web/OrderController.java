package com.aislego.orders.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.dto.OrderStatusResponse;
import com.aislego.orders.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> listMine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return orderService.listMyOrders(principal.userId());
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long orderId) {
        return orderService.getMyOrder(principal.userId(), orderId);
    }

    @GetMapping("/{orderId}/status")
    public OrderStatusResponse getStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @PathVariable Long orderId) {
        return new OrderStatusResponse(orderService.getMyOrderStatus(principal.userId(), orderId));
    }
}
