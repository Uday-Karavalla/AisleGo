package com.aislego.reviews.repository;

import com.aislego.reviews.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndSupermarketId(Long userId, Long supermarketId);

    List<Review> findBySupermarketIdOrderByCreatedAtDesc(Long supermarketId);

    /**
     * One batched aggregate query for a whole candidate set (e.g. every store in a "nearby"
     * result page) rather than one query per store - same over-fetch-then-batch shape as
     * {@code RoutingService.estimateRoutes}. A supermarket with zero reviews simply has no row
     * in the result, not a zero-valued one.
     */
    @Query("""
            select r.supermarket.id as supermarketId, avg(r.rating) as averageRating, count(r) as reviewCount
            from Review r
            where r.supermarket.id in :supermarketIds
            group by r.supermarket.id
            """)
    List<RatingSummaryView> summarizeRatings(@Param("supermarketIds") List<Long> supermarketIds);
}
