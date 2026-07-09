package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.dto.BranchResponse;
import com.aislego.catalogue.dto.NearbyBranchResponse;
import com.aislego.catalogue.dto.SupermarketResponse;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.catalogue.repository.NearbyBranchView;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.catalogue.routing.GeoPoint;
import com.aislego.catalogue.routing.RouteEstimate;
import com.aislego.catalogue.routing.RoutingService;
import com.aislego.common.exception.NotFoundException;
import com.aislego.reviews.repository.RatingSummaryView;
import com.aislego.reviews.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StoreDiscoveryService {

    private static final int DEFAULT_RESULT_LIMIT = 50;

    // Generous candidate pool: real-distance filtering (radius vs. bounding box, and
    // straight-line vs. driving distance) will drop some of these, so we over-fetch relative
    // to the final result cap.
    private static final int CANDIDATE_LIMIT = 200;

    // The bounding box side must exceed the requested radius since (a) it's a square, not a
    // circle, and (b) real driving distance is usually longer than straight-line distance.
    private static final double BOUNDING_BOX_RADIUS_MULTIPLIER = 1.5;

    private static final double KM_PER_DEGREE_LATITUDE = 111.0;

    // Guards against a division blow-up near the poles; irrelevant for AisleGo's real-world
    // usage but keeps the math well-defined.
    private static final double MIN_COS_LATITUDE = 0.01;

    private final BranchRepository branchRepository;
    private final SupermarketRepository supermarketRepository;
    private final RoutingService routingService;
    private final ReviewService reviewService;
    private final Clock clock;

    public StoreDiscoveryService(BranchRepository branchRepository,
                                  SupermarketRepository supermarketRepository,
                                  RoutingService routingService,
                                  ReviewService reviewService,
                                  Clock clock) {
        this.branchRepository = branchRepository;
        this.supermarketRepository = supermarketRepository;
        this.routingService = routingService;
        this.reviewService = reviewService;
        this.clock = clock;
    }

    public List<NearbyBranchResponse> findNearby(double lat, double lng, double radiusKm) {
        double boxRadiusKm = radiusKm * BOUNDING_BOX_RADIUS_MULTIPLIER;
        double latDelta = boxRadiusKm / KM_PER_DEGREE_LATITUDE;
        double kmPerDegreeLongitude = KM_PER_DEGREE_LATITUDE
                * Math.max(Math.cos(Math.toRadians(lat)), MIN_COS_LATITUDE);
        double lngDelta = boxRadiusKm / kmPerDegreeLongitude;

        List<NearbyBranchView> candidates = branchRepository.findNearbyBranchCandidates(
                lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta, CANDIDATE_LIMIT);

        if (candidates.isEmpty()) {
            return List.of();
        }

        GeoPoint origin = new GeoPoint(lat, lng);
        List<GeoPoint> destinations = candidates.stream()
                .map(candidate -> new GeoPoint(candidate.getLatitude(), candidate.getLongitude()))
                .toList();

        // One batched call for the whole candidate set - not one call per branch - both for
        // correctness (this is the whole point of moving distance out of per-row SQL) and to
        // stay well within a real provider's rate limits (e.g. ORS's free-tier 40 req/min).
        List<RouteEstimate> estimates = routingService.estimateRoutes(origin, destinations);

        LocalTime now = LocalTime.now(clock);

        List<Long> candidateSupermarketIds = candidates.stream().map(NearbyBranchView::getSupermarketId).distinct().toList();
        Map<Long, RatingSummaryView> ratingSummaries = reviewService.summarize(candidateSupermarketIds);

        List<NearbyBranchResponse> results = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            RouteEstimate estimate = estimates.get(i);
            if (estimate.distanceKm() > radiusKm) {
                continue;
            }
            NearbyBranchView candidate = candidates.get(i);
            results.add(NearbyBranchResponse.from(candidate, estimate, isOpen(candidate, now),
                    ratingSummaries.get(candidate.getSupermarketId())));
        }

        results.sort(Comparator.comparingDouble(NearbyBranchResponse::distanceKm));

        return results.size() > DEFAULT_RESULT_LIMIT
                ? results.subList(0, DEFAULT_RESULT_LIMIT)
                : results;
    }

    /**
     * A missing or unparseable opening/closing time is treated as "open" rather than hiding the
     * store - a data-entry gap shouldn't make a real store disappear from discovery. Compared
     * against {@link LocalTime#now(Clock)} in the JVM's default zone: a known simplification,
     * there's no per-store/per-city timezone support yet.
     */
    private boolean isOpen(NearbyBranchView branch, LocalTime now) {
        String openingTime = branch.getOpeningTime();
        String closingTime = branch.getClosingTime();
        if (openingTime == null || closingTime == null) {
            return true;
        }
        try {
            LocalTime opens = LocalTime.parse(openingTime);
            LocalTime closes = LocalTime.parse(closingTime);
            if (closes.isAfter(opens)) {
                return !now.isBefore(opens) && now.isBefore(closes);
            }
            // closing <= opening is treated as an overnight window (e.g. 22:00-02:00).
            return !now.isBefore(opens) || now.isBefore(closes);
        } catch (DateTimeParseException ex) {
            return true;
        }
    }

    public SupermarketResponse getSupermarket(Long supermarketId) {
        Supermarket supermarket = supermarketRepository.findById(supermarketId)
                .orElseThrow(() -> new NotFoundException("Supermarket " + supermarketId + " was not found"));

        if (supermarket.getStatus() != SupermarketStatus.VERIFIED) {
            throw new NotFoundException("Supermarket " + supermarketId + " was not found");
        }

        List<BranchResponse> branches = branchRepository.findBySupermarketIdAndActiveTrue(supermarketId).stream()
                .map(BranchResponse::from)
                .toList();

        RatingSummaryView ratingSummary = reviewService.summarize(List.of(supermarketId)).get(supermarketId);
        return SupermarketResponse.from(supermarket, branches, ratingSummary);
    }
}
