package com.aislego.admin.web;

import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.AdminOrderResponse;
import com.aislego.orders.service.AdminOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin orchestration layer for admin order oversight - doesn't own the {@code Order} entity
 * itself, that lives in {@code orders} (see {@code AdminOrderService}), same pattern as
 * {@link AdminSupermarketController}.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public Page<AdminOrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return adminOrderService.listAll(status, pageable);
    }
}
