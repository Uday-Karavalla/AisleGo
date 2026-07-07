package com.aislego.catalogue.routing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Zero-setup stand-in for a real routing/maps API (mirrors {@code MockPaymentGateway}): plain
 * great-circle (Haversine) distance, no external calls, no API key required. This is today's
 * behaviour, just moved out of raw SQL trig and into Java so {@link OpenRouteServiceRoutingService}
 * can share the same fallback math instead of duplicating it.
 *
 * <p>Explicitly mutually exclusive with {@link OpenRouteServiceRoutingService} via the same
 * property (matching "haversine" or unset) - without this, setting
 * {@code aislego.routing.provider=openrouteservice} would leave *both* beans active and crash
 * the app at startup with a {@code NoUniqueBeanDefinitionException}.
 */
@Service
@ConditionalOnProperty(name = "aislego.routing.provider", havingValue = "haversine", matchIfMissing = true)
public class HaversineRoutingService implements RoutingService {

    @Override
    public List<RouteEstimate> estimateRoutes(GeoPoint origin, List<GeoPoint> destinations) {
        return destinations.stream()
                .map(destination -> HaversineMath.estimate(origin, destination))
                .toList();
    }

    @Override
    public Optional<GeoPoint> geocode(String query) {
        // The default provider is pure straight-line math with no address database behind it,
        // so it has nothing to resolve a free-text query against. Only the opt-in
        // OpenRouteServiceRoutingService supports geocoding.
        return Optional.empty();
    }
}
