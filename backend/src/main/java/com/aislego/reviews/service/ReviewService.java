package com.aislego.reviews.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ForbiddenException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.repository.UserRepository;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.reviews.domain.Review;
import com.aislego.reviews.dto.MyReviewStatusResponse;
import com.aislego.reviews.dto.ReviewResponse;
import com.aislego.reviews.dto.StoreReviewsResponse;
import com.aislego.reviews.dto.SubmitReviewRequest;
import com.aislego.reviews.repository.RatingSummaryView;
import com.aislego.reviews.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SupermarketRepository supermarketRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository, SupermarketRepository supermarketRepository,
                          UserRepository userRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.supermarketRepository = supermarketRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public StoreReviewsResponse getStoreReviews(Long supermarketId) {
        requireSupermarket(supermarketId);
        List<Review> reviews = reviewRepository.findBySupermarketIdOrderByCreatedAtDesc(supermarketId);
        Map<Long, RatingSummaryView> summary = summarize(List.of(supermarketId));
        RatingSummaryView aggregate = summary.get(supermarketId);
        return new StoreReviewsResponse(
                aggregate != null ? aggregate.getAverageRating() : null,
                aggregate != null ? aggregate.getReviewCount() : 0,
                reviews.stream().map(ReviewResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public MyReviewStatusResponse getMyReviewStatus(Long userId, Long supermarketId) {
        requireSupermarket(supermarketId);
        boolean eligible = orderRepository.existsByUserIdAndSupermarketIdAndStatus(
                userId, supermarketId, OrderStatus.DELIVERED);
        Review existing = reviewRepository.findByUserIdAndSupermarketId(userId, supermarketId).orElse(null);
        return new MyReviewStatusResponse(eligible, existing != null ? ReviewResponse.from(existing) : null);
    }

    public ReviewResponse submitReview(Long userId, Long supermarketId, SubmitReviewRequest request) {
        Supermarket supermarket = requireSupermarket(supermarketId);
        boolean delivered = orderRepository.existsByUserIdAndSupermarketIdAndStatus(
                userId, supermarketId, OrderStatus.DELIVERED);
        if (!delivered) {
            throw new ForbiddenException("REVIEW_NOT_ELIGIBLE",
                    "You can review a store only after an order from it has been delivered");
        }

        Review review = reviewRepository.findByUserIdAndSupermarketId(userId, supermarketId)
                .orElseGet(() -> {
                    Review created = new Review();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setSupermarket(supermarket);
                    return created;
                });
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    /** Batched rating summary for a set of supermarkets, keyed by supermarket id - used by
     *  {@code StoreDiscoveryService} so a "nearby" page issues one aggregate query for the
     *  whole result set instead of one per store. */
    @Transactional(readOnly = true)
    public Map<Long, RatingSummaryView> summarize(List<Long> supermarketIds) {
        if (supermarketIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.summarizeRatings(supermarketIds).stream()
                .collect(Collectors.toMap(RatingSummaryView::getSupermarketId, Function.identity()));
    }

    private Supermarket requireSupermarket(Long supermarketId) {
        return supermarketRepository.findById(supermarketId)
                .orElseThrow(() -> new NotFoundException("Supermarket " + supermarketId + " was not found"));
    }
}
