package com.aislego.catalogue.routing;

/**
 * Distance/duration estimate for a single origin-to-destination leg. {@code distanceKm} is a
 * real driving distance when the OpenRouteService provider is enabled, or a great-circle
 * (straight-line) estimate from {@link HaversineRoutingService} otherwise; {@code
 * durationMinutes} follows the same distinction.
 */
public record RouteEstimate(double distanceKm, double durationMinutes) {
}
