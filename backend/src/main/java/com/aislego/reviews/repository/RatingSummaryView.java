package com.aislego.reviews.repository;

/**
 * Interface-backed projection for the batched per-supermarket rating aggregate in
 * {@link ReviewRepository#summarizeRatings}. Field names must match the JPQL query's aliases.
 */
public interface RatingSummaryView {
    Long getSupermarketId();

    Double getAverageRating();

    Long getReviewCount();
}
