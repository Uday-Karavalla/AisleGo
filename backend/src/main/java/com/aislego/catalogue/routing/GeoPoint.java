package com.aislego.catalogue.routing;

/**
 * A plain latitude/longitude coordinate pair, decoupled from any particular provider's wire
 * format (e.g. OpenRouteService's {@code [lng, lat]} ordering) so callers never have to think
 * about coordinate order outside {@link RoutingService} implementations.
 */
public record GeoPoint(double lat, double lng) {
}
