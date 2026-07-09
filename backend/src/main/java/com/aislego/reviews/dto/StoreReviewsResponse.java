package com.aislego.reviews.dto;

import java.util.List;

/** {@code averageRating} is null, not zero, when {@code reviewCount} is 0 - a store with no
 *  reviews yet has no rating to show, rather than an implied 0-star one. */
public record StoreReviewsResponse(
        Double averageRating,
        long reviewCount,
        List<ReviewResponse> reviews
) {
}
