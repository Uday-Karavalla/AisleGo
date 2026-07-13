package com.aislego.growth.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.growth.dto.ReferralSummaryResponse;
import com.aislego.growth.service.ReferralService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReferralController {
    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping("/mine")
    public ReferralSummaryResponse mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return referralService.getSummary(principal.userId());
    }
}
