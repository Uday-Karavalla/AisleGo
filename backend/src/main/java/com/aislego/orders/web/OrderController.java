package com.aislego.orders.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.dto.OrderStatusResponse;
import com.aislego.orders.service.OrderService;
import com.aislego.orders.service.ReorderService;
import com.aislego.orders.dto.CartResponse;
import com.aislego.delivery.dto.DeliveryLocationResponse;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final ReorderService reorderService;

    public OrderController(OrderService orderService, ReorderService reorderService) {
        this.orderService = orderService;
        this.reorderService = reorderService;
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

    @GetMapping("/{orderId}/delivery-location")
    public DeliveryLocationResponse getDeliveryLocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long orderId) {
        return orderService.getMyDeliveryLocation(principal.userId(), orderId);
    }

    @PostMapping("/{orderId}/reorder")
    public CartResponse reorder(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long orderId) {
        return reorderService.reorder(principal.userId(), orderId);
    }
}
