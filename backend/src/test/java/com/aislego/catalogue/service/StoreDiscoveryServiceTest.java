package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Category;
import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.dto.BranchDetailResponse;
import com.aislego.catalogue.dto.CategoryProductResponse;
import com.aislego.catalogue.dto.NearbyBranchResponse;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.catalogue.repository.NearbyBranchView;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.catalogue.routing.RouteEstimate;
import com.aislego.catalogue.routing.RoutingService;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.reviews.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreDiscoveryServiceTest {

    @Mock
    private BranchRepository branchRepository;
    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private RoutingService routingService;
    @Mock
    private ReviewService reviewService;

    private static final double ORIGIN_LAT = 12.9716;
    private static final double ORIGIN_LNG = 77.6412;
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        lenient().when(reviewService.summarize(any())).thenReturn(Map.of());
    }

    private StoreDiscoveryService serviceWithClockAt(String isoLocalTime) {
        Instant fixedInstant = OffsetDateTime.parse("2026-07-03T" + isoLocalTime + ":00+05:30").toInstant();
        Clock clock = Clock.fixed(fixedInstant, ZONE);
        return new StoreDiscoveryService(branchRepository, supermarketRepository, productRepository, routingService,
                reviewService, clock);
    }

    private TestBranchView candidate(long id, String openingTime, String closingTime) {
        return new TestBranchView(id, "Branch " + id, "Address " + id, "Bengaluru",
                12.95, 77.63, 1L, "Supermarket", openingTime, closingTime);
    }

    @Test
    void filtersOutCandidatesInsideTheBoundingBoxButOutsideTheRealDistanceRadius() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        TestBranchView near = candidate(1L, "09:00", "21:00");
        TestBranchView far = candidate(2L, "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(near, far));
        when(routingService.estimateRoutes(any(), any()))
                .thenReturn(List.of(new RouteEstimate(3.0, 10.0), new RouteEstimate(8.0, 25.0)));

        List<NearbyBranchResponse> results = service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 5.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).branchId()).isEqualTo(1L);
        assertThat(results.get(0).distanceKm()).isEqualTo(3.0);
        assertThat(results.get(0).etaMinutes()).isEqualTo(10.0);
    }

    @Test
    void sortsResultsByDistanceAscendingRegardlessOfCandidateOrder() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        TestBranchView branchA = candidate(1L, "09:00", "21:00");
        TestBranchView branchB = candidate(2L, "09:00", "21:00");
        TestBranchView branchC = candidate(3L, "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(branchA, branchB, branchC));
        // Estimates deliberately out of order relative to distance so sorting is exercised.
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(
                new RouteEstimate(4.0, 12.0),
                new RouteEstimate(1.0, 4.0),
                new RouteEstimate(2.5, 8.0)));

        List<NearbyBranchResponse> results = service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 10.0);

        assertThat(results).extracting(NearbyBranchResponse::branchId).containsExactly(2L, 3L, 1L);
        assertThat(results).extracting(NearbyBranchResponse::distanceKm).containsExactly(1.0, 2.5, 4.0);
    }

    @Test
    void marksABranchOpenWhenCurrentTimeIsWithinItsOpeningHours() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        TestBranchView branch = candidate(1L, "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(branch));
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(new RouteEstimate(1.0, 3.0)));

        List<NearbyBranchResponse> results = service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 10.0);

        assertThat(results.get(0).isOpen()).isTrue();
    }

    @Test
    void marksABranchClosedWhenCurrentTimeIsOutsideItsOpeningHours() {
        StoreDiscoveryService service = serviceWithClockAt("23:00");

        TestBranchView branch = candidate(1L, "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(branch));
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(new RouteEstimate(1.0, 3.0)));

        List<NearbyBranchResponse> results = service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 10.0);

        assertThat(results.get(0).isOpen()).isFalse();
    }

    @Test
    void treatsMissingOpeningHoursAsOpenRatherThanHidingTheStore() {
        StoreDiscoveryService service = serviceWithClockAt("23:00");

        TestBranchView branch = candidate(1L, null, null);
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(branch));
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(new RouteEstimate(1.0, 3.0)));

        List<NearbyBranchResponse> results = service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 10.0);

        assertThat(results.get(0).isOpen()).isTrue();
    }

    @Test
    void callsRoutingServiceExactlyOnceForTheWholeCandidateBatch() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        TestBranchView branchA = candidate(1L, "09:00", "21:00");
        TestBranchView branchB = candidate(2L, "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(branchA, branchB));
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(
                new RouteEstimate(1.0, 3.0), new RouteEstimate(2.0, 6.0)));

        service.findNearby(ORIGIN_LAT, ORIGIN_LNG, 10.0);

        org.mockito.Mockito.verify(routingService, org.mockito.Mockito.times(1))
                .estimateRoutes(any(), any());
    }

    private Branch branchOf(Long branchId, Supermarket supermarket) {
        Branch branch = new Branch();
        branch.setId(branchId);
        branch.setSupermarket(supermarket);
        branch.setName("Branch " + branchId);
        branch.setAddressLine("Address " + branchId);
        branch.setCity("Bengaluru");
        branch.setLatitude(12.95);
        branch.setLongitude(77.63);
        branch.setOpeningTime("09:00");
        branch.setClosingTime("21:00");
        branch.setActive(true);
        return branch;
    }

    @Test
    void getBranchDetailResolvesTheSupermarketIdSeparatelyFromTheBranchId() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        Supermarket supermarket = new Supermarket();
        supermarket.setId(7L);
        supermarket.setName("ValueMart");
        supermarket.setStatus(SupermarketStatus.VERIFIED);
        Branch branch = branchOf(5L, supermarket);

        when(branchRepository.findById(5L)).thenReturn(Optional.of(branch));

        BranchDetailResponse result = service.getBranchDetail(5L);

        assertThat(result.branchId()).isEqualTo(5L);
        assertThat(result.supermarketId()).isEqualTo(7L);
        assertThat(result.isOpen()).isTrue();
    }

    @Test
    void getBranchDetailThrowsNotFoundForAnInactiveBranch() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        Supermarket supermarket = new Supermarket();
        supermarket.setId(7L);
        supermarket.setStatus(SupermarketStatus.VERIFIED);
        Branch branch = branchOf(5L, supermarket);
        branch.setActive(false);

        when(branchRepository.findById(5L)).thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> service.getBranchDetail(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getBranchDetailThrowsNotFoundWhenTheSupermarketIsNotVerified() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        Supermarket supermarket = new Supermarket();
        supermarket.setId(7L);
        supermarket.setStatus(SupermarketStatus.PENDING);
        Branch branch = branchOf(5L, supermarket);

        when(branchRepository.findById(5L)).thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> service.getBranchDetail(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void browseCategoryNearbyPinsEachProductToItsNearestNearbyBranch() {
        StoreDiscoveryService service = serviceWithClockAt("12:00");

        TestBranchView nearBranchOfSupermarketSeven = new TestBranchView(
                10L, "Corner Grocer Main", "Addr", "Bengaluru", 12.95, 77.63, 7L, "Corner Grocer",
                "09:00", "21:00");
        TestBranchView otherBranchOfSupermarketNine = new TestBranchView(
                20L, "ValueMart Annex", "Addr", "Bengaluru", 12.95, 77.63, 9L, "ValueMart",
                "09:00", "21:00");
        when(branchRepository.findNearbyBranchCandidates(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(nearBranchOfSupermarketSeven, otherBranchOfSupermarketNine));
        when(routingService.estimateRoutes(any(), any())).thenReturn(List.of(
                new RouteEstimate(1.0, 3.0), new RouteEstimate(2.0, 6.0)));

        Supermarket supermarketSeven = new Supermarket();
        supermarketSeven.setId(7L);
        supermarketSeven.setName("Corner Grocer");
        Product carrot = new Product();
        carrot.setId(100L);
        carrot.setName("Carrot (1 kg)");
        carrot.setSku("CG-CARROT");
        carrot.setPrice(Money.of(new BigDecimal("40.00"), "INR"));
        carrot.setSupermarket(supermarketSeven);
        Category vegetables = new Category();
        vegetables.setName("Fruits & Vegetables");
        carrot.setCategory(vegetables);

        when(productRepository.searchByCategoryKeywordAcrossSupermarkets(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(carrot), PageRequest.of(0, 20), 1));

        var result = service.browseCategoryNearby("vegetable", ORIGIN_LAT, ORIGIN_LNG, 10.0, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        CategoryProductResponse response = result.getContent().get(0);
        assertThat(response.supermarketId()).isEqualTo(7L);
        assertThat(response.branchId()).isEqualTo(10L);
        assertThat(response.distanceKm()).isEqualTo(1.0);
        assertThat(response.categoryName()).isEqualTo("Fruits & Vegetables");
    }

    private record TestBranchView(Long id, String name, String addressLine, String city, Double latitude,
                                   Double longitude, Long supermarketId, String supermarketName,
                                   String openingTime, String closingTime) implements NearbyBranchView {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAddressLine() {
            return addressLine;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public Double getLatitude() {
            return latitude;
        }

        @Override
        public Double getLongitude() {
            return longitude;
        }

        @Override
        public Long getSupermarketId() {
            return supermarketId;
        }

        @Override
        public String getSupermarketName() {
            return supermarketName;
        }

        @Override
        public String getOpeningTime() {
            return openingTime;
        }

        @Override
        public String getClosingTime() {
            return closingTime;
        }
    }
}
