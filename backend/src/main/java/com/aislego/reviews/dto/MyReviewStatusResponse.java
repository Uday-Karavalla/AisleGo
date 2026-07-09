package com.aislego.reviews.dto;

/** {@code eligible} is true once the caller has at least one delivered order from this store -
 *  the frontend uses it to decide whether to show "Write a review" at all. {@code myReview} is
 *  null until they've actually submitted one, regardless of eligibility. */
public record MyReviewStatusResponse(
        boolean eligible,
        ReviewResponse myReview
) {
}
