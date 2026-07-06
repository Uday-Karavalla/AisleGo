package com.aislego.catalogue.routing;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over "however we turn coordinates into distance/duration and addresses into
 * coordinates." {@link HaversineRoutingService} is the zero-setup default (great-circle math,
 * no external calls); {@link OpenRouteServiceRoutingService} is the opt-in real-routing
 * implementation, mirroring the {@code PaymentService} seam (mock default + opt-in real
 * provider via {@code @ConditionalOnProperty}).
 */
public interface RoutingService {

    /**
     * Estimates a route from {@code origin} to each of {@code destinations}, in one batched
     * call. The returned list is the same size and order as {@code destinations} - result
     * {@code i} corresponds to {@code destinations.get(i)}.
     */
    List<RouteEstimate> estimateRoutes(GeoPoint origin, List<GeoPoint> destinations);

    /**
     * Resolves a free-text address/place query to coordinates. Returns {@link Optional#empty()}
     * when the provider doesn't support geocoding at all, or when it found no match for the
     * query - callers can't distinguish the two, which is fine since both mean "we don't have
     * coordinates for this text."
     */
    Optional<GeoPoint> geocode(String query);
}
