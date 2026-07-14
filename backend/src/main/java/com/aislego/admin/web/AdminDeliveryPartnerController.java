package com.aislego.admin.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.delivery.domain.DeliveryPartnerStatus;
import com.aislego.delivery.dto.AdminDeliveryPartnerResponse;
import com.aislego.delivery.dto.RejectDeliveryPartnerRequest;
import com.aislego.delivery.service.DeliveryPartnerVerificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/delivery-partners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryPartnerController {
    private final DeliveryPartnerVerificationService service;

    public AdminDeliveryPartnerController(DeliveryPartnerVerificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminDeliveryPartnerResponse> list(@RequestParam(required = false) DeliveryPartnerStatus status) {
        return service.list(status);
    }

    @PostMapping("/{id}/verify")
    public void verify(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        service.verify(id, principal.userId());
    }

    @PostMapping("/{id}/reject")
    public void reject(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id,
                       @Valid @RequestBody RejectDeliveryPartnerRequest request) {
        service.reject(id, principal.userId(), request.reason());
    }
}
