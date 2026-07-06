package com.aislego.catalogue.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Mocking Spring's {@link RestClient} fluent builder chain directly is awkward (it's a long
 * chain of interfaces), so instead these tests use the package-private
 * {@code callMatrixApi}/{@code callGeocodeApi} seam: a throwaway subclass overrides just the
 * "make the HTTP call" step, letting these tests exercise the real response-parsing and
 * fallback logic without a real or fake HTTP server. The {@link RestClient} passed to the
 * package-private constructor is never actually used since every test overrides both call
 * methods.
 */
class OpenRouteServiceRoutingServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final GeoPoint ORIGIN = new GeoPoint(12.9716, 77.6412);
    private static final GeoPoint DESTINATION = new GeoPoint(12.9352, 77.6146);
    private static final RestClient UNUSED_REST_CLIENT = RestClient.create();

    @Test
    void estimateRoutesFallsBackToHaversineWhenTheMatrixCallFails() {
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callMatrixApi(GeoPoint origin, List<GeoPoint> destinations) {
                throw new RuntimeException("simulated ORS outage");
            }
        };

        List<RouteEstimate> estimates = service.estimateRoutes(ORIGIN, List.of(DESTINATION));

        RouteEstimate expected = HaversineMath.estimate(ORIGIN, DESTINATION);
        assertThat(estimates).hasSize(1);
        assertThat(estimates.get(0).distanceKm()).isCloseTo(expected.distanceKm(), within(0.0001));
        assertThat(estimates.get(0).durationMinutes()).isCloseTo(expected.durationMinutes(), within(0.0001));
    }

    @Test
    void estimateRoutesReturnsEmptyListWithoutCallingApiWhenThereAreNoDestinations() {
        AtomicBoolean called = new AtomicBoolean(false);
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callMatrixApi(GeoPoint origin, List<GeoPoint> destinations) {
                called.set(true);
                throw new AssertionError("callMatrixApi should not be invoked with no destinations");
            }
        };

        List<RouteEstimate> estimates = service.estimateRoutes(ORIGIN, List.of());

        assertThat(estimates).isEmpty();
        assertThat(called).isFalse();
    }

    @Test
    void estimateRoutesParsesASuccessfulMatrixResponseInMetresAndSeconds() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "distances": [[4969.0, 1000.0]],
                  "durations": [[820.0, 200.0]]
                }
                """);
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callMatrixApi(GeoPoint origin, List<GeoPoint> destinations) {
                return response;
            }
        };

        List<RouteEstimate> estimates = service.estimateRoutes(ORIGIN, List.of(DESTINATION, ORIGIN));

        assertThat(estimates).hasSize(2);
        assertThat(estimates.get(0).distanceKm()).isCloseTo(4.969, within(0.0001));
        assertThat(estimates.get(0).durationMinutes()).isCloseTo(820.0 / 60.0, within(0.0001));
        assertThat(estimates.get(1).distanceKm()).isCloseTo(1.0, within(0.0001));
        assertThat(estimates.get(1).durationMinutes()).isCloseTo(200.0 / 60.0, within(0.0001));
    }

    @Test
    void estimateRoutesFallsBackWhenTheMatrixResponseIsMalformed() throws Exception {
        JsonNode response = MAPPER.readTree("{\"distances\": null, \"durations\": null}");
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callMatrixApi(GeoPoint origin, List<GeoPoint> destinations) {
                return response;
            }
        };

        List<RouteEstimate> estimates = service.estimateRoutes(ORIGIN, List.of(DESTINATION));

        RouteEstimate expected = HaversineMath.estimate(ORIGIN, DESTINATION);
        assertThat(estimates).hasSize(1);
        assertThat(estimates.get(0).distanceKm()).isCloseTo(expected.distanceKm(), within(0.0001));
    }

    @Test
    void geocodeReturnsEmptyWhenTheApiCallFails() {
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callGeocodeApi(String query) {
                throw new RuntimeException("simulated ORS outage");
            }
        };

        assertThat(service.geocode("nowhere")).isEmpty();
    }

    @Test
    void geocodeReturnsEmptyWhenNoFeaturesMatch() throws Exception {
        JsonNode response = MAPPER.readTree("{\"features\": []}");
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callGeocodeApi(String query) {
                return response;
            }
        };

        assertThat(service.geocode("nonexistent place")).isEmpty();
    }

    @Test
    void geocodeParsesTheFirstFeatureCoordinatesFromLngLatToGeoPoint() throws Exception {
        JsonNode response = MAPPER.readTree("""
                {
                  "features": [
                    { "geometry": { "coordinates": [77.6412, 12.9716] } }
                  ]
                }
                """);
        OpenRouteServiceRoutingService service = new OpenRouteServiceRoutingService("fake-key", UNUSED_REST_CLIENT) {
            @Override
            JsonNode callGeocodeApi(String query) {
                return response;
            }
        };

        Optional<GeoPoint> result = service.geocode("100 Ft Road, Indiranagar, Bengaluru");

        assertThat(result).contains(new GeoPoint(12.9716, 77.6412));
    }
}
