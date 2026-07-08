package com.aislego.catalogue.routing;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Real routing/geocoding via OpenRouteService (ORS): the Matrix API for driving distance/
 * duration and the Geocode API for turning a free-text address into coordinates. Opt-in via
 * {@code aislego.routing.provider=openrouteservice}; {@link HaversineRoutingService} remains the
 * default so discovery works with zero setup.
 *
 * <p>Uses Spring's {@link RestClient} (already on the classpath via
 * {@code spring-boot-starter-web}) rather than a dedicated ORS SDK - no new HTTP dependency
 * needed for a single small provider integration.
 *
 * <p>{@link #estimateRoutes} wraps the whole ORS call in a try/catch: any network error,
 * non-2xx response (including a free-tier rate limit), or malformed/empty result falls back to
 * the same Haversine math {@link HaversineRoutingService} uses, via the shared
 * {@link HaversineMath} helper - so an ORS outage or rate limit degrades discovery gracefully
 * instead of breaking it.
 */
@Service
@ConditionalOnProperty(name = "aislego.routing.provider", havingValue = "openrouteservice")
public class OpenRouteServiceRoutingService implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouteServiceRoutingService.class);
    private static final String BASE_URL = "https://api.openrouteservice.org";

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public OpenRouteServiceRoutingService(@Value("${aislego.routing.openrouteservice.api-key}") String apiKey) {
        this(apiKey, RestClient.builder().baseUrl(BASE_URL).build());
    }

    /**
     * Package-private constructor so tests can inject a {@link RestClient} pointed at a fake
     * server instead of the real ORS API.
     */
    OpenRouteServiceRoutingService(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    @Override
    public List<RouteEstimate> estimateRoutes(GeoPoint origin, List<GeoPoint> destinations) {
        if (destinations.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode root = callMatrixApi(origin, destinations);
            JsonNode distancesRow = root.path("distances").path(0);
            JsonNode durationsRow = root.path("durations").path(0);
            if (!distancesRow.isArray() || !durationsRow.isArray()
                    || distancesRow.size() != destinations.size() || durationsRow.size() != destinations.size()) {
                throw new IllegalStateException("ORS matrix response missing/malformed distances or durations");
            }

            List<RouteEstimate> estimates = new ArrayList<>(destinations.size());
            for (int i = 0; i < destinations.size(); i++) {
                double distanceKm = distancesRow.get(i).asDouble() / 1000.0;
                double durationMinutes = durationsRow.get(i).asDouble() / 60.0;
                estimates.add(new RouteEstimate(distanceKm, durationMinutes));
            }
            return estimates;
        } catch (Exception ex) {
            log.warn("ORS matrix API call failed, falling back to Haversine estimate for {} destination(s): {}",
                    destinations.size(), ex.getMessage());
            return destinations.stream().map(destination -> HaversineMath.estimate(origin, destination)).toList();
        }
    }

    /**
     * Package-private seam for tests: calls ORS's Matrix API and returns the raw JSON body.
     * Overridable/spy-able so tests can simulate an ORS failure without standing up a fake
     * HTTP server.
     */
    JsonNode callMatrixApi(GeoPoint origin, List<GeoPoint> destinations) {
        List<List<Double>> locations = new ArrayList<>();
        locations.add(List.of(origin.lng(), origin.lat())); // ORS uses [lng, lat] order
        destinations.forEach(destination -> locations.add(List.of(destination.lng(), destination.lat())));

        List<Integer> destinationIndexes = IntStream.rangeClosed(1, destinations.size()).boxed().toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locations", locations);
        body.put("sources", List.of(0));
        body.put("destinations", destinationIndexes);
        body.put("metrics", List.of("distance", "duration"));

        return restClient.post()
                .uri("/v2/matrix/driving-car")
                .header("Authorization", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public Optional<GeoPoint> geocode(String query) {
        JsonNode root;
        try {
            root = callGeocodeApi(query);
        } catch (Exception ex) {
            log.warn("ORS geocode API call failed for query '{}': {}", query, ex.getMessage());
            return Optional.empty();
        }

        JsonNode features = root.path("features");
        if (!features.isArray() || features.isEmpty()) {
            // No match found - not an error, just nothing to return.
            return Optional.empty();
        }

        JsonNode coordinates = features.get(0).path("geometry").path("coordinates");
        double lng = coordinates.get(0).asDouble();
        double lat = coordinates.get(1).asDouble();
        return Optional.of(new GeoPoint(lat, lng));
    }

    /**
     * Package-private seam for tests: calls ORS's Geocode API and returns the raw JSON body.
     */
    JsonNode callGeocodeApi(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/geocode/search")
                        .queryParam("api_key", apiKey)
                        .queryParam("text", query)
                        .queryParam("size", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
}
