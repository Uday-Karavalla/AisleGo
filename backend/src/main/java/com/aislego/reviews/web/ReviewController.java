package com.aislego.reviews.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.reviews.dto.MyReviewStatusResponse;
import com.aislego.reviews.dto.ReviewResponse;
import com.aislego.reviews.dto.StoreReviewsResponse;
import com.aislego.reviews.dto.SubmitReviewRequest;
import com.aislego.reviews.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores/{supermarketId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Public - anyone browsing a store can see its reviews, signed in or not. */
    @GetMapping
    public StoreReviewsResponse list(@PathVariable Long supermarketId) {
        return reviewService.getStoreReviews(supermarketId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public MyReviewStatusResponse mine(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable Long supermarketId) {
        return reviewService.getMyReviewStatus(principal.userId(), supermarketId);
    }

    @PutMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse submit(@AuthenticationPrincipal AuthenticatedUser principal,
                                  @PathVariable Long supermarketId,
                                  @Valid @RequestBody SubmitReviewRequest request) {
        return reviewService.submitReview(principal.userId(), supermarketId, request);
    }
}
