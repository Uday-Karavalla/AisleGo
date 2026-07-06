package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.GeocodeResponse;
import com.aislego.catalogue.routing.GeoPoint;
import com.aislego.catalogue.routing.RoutingService;
import com.aislego.common.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resolves a free-text address to coordinates, primarily for the frontend's manual-address
 * entry path (which previously sent {@code lat=0, lng=0} for any typed address instead of
 * geocoding it). Lives under {@code /api/stores/**}, already {@code permitAll} for GET in
 * {@code SecurityConfig} - no security config change needed.
 */
@RestController
public class GeocodeController {

    private final RoutingService routingService;

    public GeocodeController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/api/stores/geocode")
    public GeocodeResponse geocode(@RequestParam String query) {
        GeoPoint point = routingService.geocode(query)
                .orElseThrow(() -> new NotFoundException(
                        "Could not resolve that address - try enabling location instead"));
        return new GeocodeResponse(point.lat(), point.lng());
    }
}
