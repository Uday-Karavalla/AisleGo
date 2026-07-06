package com.aislego.catalogue.routing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineRoutingServiceTest {

    // FreshMart Indiranagar / GreenBasket Koramangala coordinates from the demo seed data
    // (V2__seed_demo_data.sql), ~4.97km apart by the great-circle formula.
    private static final GeoPoint INDIRANAGAR = new GeoPoint(12.9716, 77.6412);
    private static final GeoPoint KORAMANGALA = new GeoPoint(12.9352, 77.6146);
    private static final double EXPECTED_DISTANCE_KM = 4.969;

    private final HaversineRoutingService routingService = new HaversineRoutingService();

    @Test
    void estimateRoutesComputesGreatCircleDistanceWithinTolerance() {
        List<RouteEstimate> estimates = routingService.estimateRoutes(INDIRANAGAR, List.of(KORAMANGALA));

        assertThat(estimates).hasSize(1);
        assertThat(estimates.get(0).distanceKm()).isCloseTo(EXPECTED_DISTANCE_KM, within(0.1));
    }

    @Test
    void estimateRoutesDerivesDurationFromAssumedAverageSpeed() {
        List<RouteEstimate> estimates = routingService.estimateRoutes(INDIRANAGAR, List.of(KORAMANGALA));

        RouteEstimate estimate = estimates.get(0);
        double expectedMinutes = estimate.distanceKm() / 22.0 * 60.0;
        assertThat(estimate.durationMinutes()).isCloseTo(expectedMinutes, within(0.0001));
    }

    @Test
    void estimateRoutesPreservesDestinationOrder() {
        List<RouteEstimate> estimates = routingService.estimateRoutes(INDIRANAGAR, List.of(KORAMANGALA, INDIRANAGAR));

        assertThat(estimates).hasSize(2);
        assertThat(estimates.get(1).distanceKm()).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void geocodeIsNotSupportedByTheDefaultProvider() {
        Optional<GeoPoint> result = routingService.geocode("100 Ft Road, Indiranagar, Bengaluru");

        assertThat(result).isEmpty();
    }
}
