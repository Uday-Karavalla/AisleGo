package com.aislego.growth.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.growth.dto.GrowthDashboardResponse;
import com.aislego.growth.dto.TrackEventRequest;
import com.aislego.growth.service.GrowthAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/growth")
public class GrowthAnalyticsController {
    private final GrowthAnalyticsService service;
    public GrowthAnalyticsController(GrowthAnalyticsService service) { this.service = service; }

    @PostMapping("/events")
    public ResponseEntity<Void> track(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @Valid @RequestBody TrackEventRequest request) {
        service.track(principal == null ? null : principal.userId(), request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public GrowthDashboardResponse dashboard(@RequestParam(defaultValue = "30") int days) {
        return service.dashboard(days);
    }
}
