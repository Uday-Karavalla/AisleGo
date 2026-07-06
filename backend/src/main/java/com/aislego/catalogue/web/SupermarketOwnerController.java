package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.MySupermarketResponse;
import com.aislego.catalogue.service.SupermarketVerificationService;
import com.aislego.common.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supermarkets")
@PreAuthorize("hasRole('SUPERMARKET_OWNER')")
public class SupermarketOwnerController {

    private final SupermarketVerificationService verificationService;

    public SupermarketOwnerController(SupermarketVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Lets a supermarket owner check their own application's review status
     * (PENDING/VERIFIED/REJECTED) and, if rejected, the reason.
     */
    @GetMapping("/mine")
    public ResponseEntity<MySupermarketResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        var supermarket = verificationService.getMine(principal.userId());
        return ResponseEntity.ok(MySupermarketResponse.from(supermarket));
    }
}
