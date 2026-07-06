package com.aislego.admin.web;

import com.aislego.admin.dto.RejectSupermarketRequest;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.dto.PendingSupermarketResponse;
import com.aislego.catalogue.service.SupermarketVerificationService;
import com.aislego.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Thin orchestration layer for the admin review workflow - doesn't own the {@code Supermarket}
 * entity itself, that lives in {@code catalogue} (see {@code SupermarketVerificationService}).
 */
@RestController
@RequestMapping("/api/admin/supermarkets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupermarketController {

    private final SupermarketVerificationService verificationService;

    public AdminSupermarketController(SupermarketVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** Omit {@code status} for the full store directory; pass it to filter to one review state. */
    @GetMapping
    public ResponseEntity<List<PendingSupermarketResponse>> list(
            @RequestParam(required = false) SupermarketStatus status) {
        return ResponseEntity.ok(verificationService.listByStatus(status));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> verify(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        verificationService.verify(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id,
                                        @Valid @RequestBody RejectSupermarketRequest request) {
        verificationService.reject(id, principal.userId(), request.reason());
        return ResponseEntity.noContent().build();
    }
}
