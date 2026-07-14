package com.aislego.delivery.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.delivery.dto.DeliveryPartnerProfileResponse;
import com.aislego.delivery.dto.UpdateAvailabilityRequest;
import com.aislego.delivery.dto.DeliveryOfferResponse;
import com.aislego.delivery.dto.VerifyHandoffOtpRequest;
import com.aislego.delivery.dto.UpdateDeliveryLocationRequest;
import com.aislego.delivery.dto.DeliveryLocationResponse;
import com.aislego.delivery.dto.DeliveryHistoryResponse;
import com.aislego.delivery.dto.DeliveryEarningsResponse;
import com.aislego.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-partner")
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class DeliveryPartnerController {
    private final DeliveryPartnerService service;

    public DeliveryPartnerController(DeliveryPartnerService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public DeliveryPartnerProfileResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.getProfile(principal.userId());
    }

    @PatchMapping("/availability")
    public DeliveryPartnerProfileResponse updateAvailability(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @Valid @RequestBody UpdateAvailabilityRequest request) {
        return service.updateAvailability(principal.userId(), request.available());
    }

    @GetMapping("/offers")
    public List<DeliveryOfferResponse> offers(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.listOffers(principal.userId());
    }

    @GetMapping("/active")
    public DeliveryOfferResponse active(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.getActiveDelivery(principal.userId());
    }

    @PostMapping("/offers/{orderId}/accept")
    public DeliveryOfferResponse accept(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable Long orderId) {
        return service.acceptOffer(principal.userId(), orderId);
    }

    @PostMapping("/deliveries/{orderId}/pickup/verify")
    public DeliveryOfferResponse verifyPickup(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long orderId,
                                               @Valid @RequestBody VerifyHandoffOtpRequest request) {
        return service.verifyPickup(principal.userId(), orderId, request.code());
    }

    @PostMapping("/deliveries/{orderId}/start")
    public DeliveryOfferResponse start(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable Long orderId) {
        return service.startDelivery(principal.userId(), orderId);
    }

    @PostMapping("/deliveries/{orderId}/delivery/verify")
    public DeliveryOfferResponse verifyDelivery(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long orderId,
                                                 @Valid @RequestBody VerifyHandoffOtpRequest request) {
        return service.verifyDelivery(principal.userId(), orderId, request.code());
    }

    @PatchMapping("/deliveries/{orderId}/location")
    public DeliveryLocationResponse updateLocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable Long orderId,
                                                    @Valid @RequestBody UpdateDeliveryLocationRequest request) {
        return service.updateLocation(principal.userId(), orderId, request.latitude(), request.longitude());
    }

    @GetMapping("/history")
    public List<DeliveryHistoryResponse> history(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.history(principal.userId());
    }

    @GetMapping("/earnings")
    public DeliveryEarningsResponse earnings(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.earnings(principal.userId());
    }
}
