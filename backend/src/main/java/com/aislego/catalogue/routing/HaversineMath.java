package com.aislego.catalogue.routing;

/**
 * Great-circle distance math shared by {@link HaversineRoutingService} (the default provider)
 * and {@link OpenRouteServiceRoutingService} (as its fallback when a real ORS call fails).
 * Package-private: this is a shared implementation detail, not something outside {@code
 * catalogue.routing} should ever call directly. Kept as a static helper rather than injecting
 * {@link HaversineRoutingService} as a bean into {@link OpenRouteServiceRoutingService}, since
 * both classes implement {@link RoutingService} and injecting one concrete implementation into
 * another would create an ambiguous-bean wiring problem.
 */
final class HaversineMath {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Assumed average urban driving speed used to turn a straight-line distance into a rough
     * ETA when no real routing/traffic data is available. This is a coarse approximation
     * carried over from the original SQL-based "nearby stores" query, not a substitute for
     * real traffic-aware routing.
     */
    static final double ASSUMED_AVERAGE_SPEED_KMH = 22.0;

    private HaversineMath() {
    }

    /**
     * Ports the Haversine/spherical-law-of-cosines formula that previously lived in {@code
     * BranchRepository}'s native SQL query into Java.
     */
    static double distanceKm(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.lat());
        double lat2 = Math.toRadians(to.lat());
        double lngDiff = Math.toRadians(to.lng() - from.lng());

        double cosCentralAngle = Math.cos(lat1) * Math.cos(lat2) * Math.cos(lngDiff)
                + Math.sin(lat1) * Math.sin(lat2);
        // Clamp to [-1, 1] - acos is undefined outside that range, and floating-point drift can
        // push the cosine of a near-zero angle slightly past 1.0. Mirrors the LEAST/GREATEST
        // clamp in the old native SQL query.
        double clamped = Math.max(-1.0, Math.min(1.0, cosCentralAngle));
        return EARTH_RADIUS_KM * Math.acos(clamped);
    }

    static RouteEstimate estimate(GeoPoint from, GeoPoint to) {
        double distanceKm = distanceKm(from, to);
        double durationMinutes = distanceKm / ASSUMED_AVERAGE_SPEED_KMH * 60.0;
        return new RouteEstimate(distanceKm, durationMinutes);
    }
}
