package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Cheap candidate lookup for "nearby stores": a lat/lng bounding-box filter, backed by the
     * {@code idx_branches_lat_lng} index (see V1 migration). This intentionally over-selects -
     * a bounding box is a square, not a circle, so it includes some branches outside the real
     * radius - real distance/duration filtering happens afterwards in
     * {@code StoreDiscoveryService} via {@code RoutingService}, which also handles the fact that
     * driving distance isn't the same as straight-line distance.
     */
    @Query(value = """
            SELECT
                b.id AS id,
                b.name AS name,
                b.address_line AS addressLine,
                b.city AS city,
                b.latitude AS latitude,
                b.longitude AS longitude,
                b.supermarket_id AS supermarketId,
                s.name AS supermarketName,
                b.opening_time AS openingTime,
                b.closing_time AS closingTime
            FROM branches b
            JOIN supermarkets s ON s.id = b.supermarket_id
            WHERE b.active = true AND s.active = true AND s.status = 'VERIFIED'
              AND b.latitude BETWEEN :latMin AND :latMax
              AND b.longitude BETWEEN :lngMin AND :lngMax
            LIMIT :limit
            """, nativeQuery = true)
    List<NearbyBranchView> findNearbyBranchCandidates(@Param("latMin") double latMin,
                                                        @Param("latMax") double latMax,
                                                        @Param("lngMin") double lngMin,
                                                        @Param("lngMax") double lngMax,
                                                        @Param("limit") int limit);

    List<Branch> findBySupermarketIdAndActiveTrue(Long supermarketId);
}
