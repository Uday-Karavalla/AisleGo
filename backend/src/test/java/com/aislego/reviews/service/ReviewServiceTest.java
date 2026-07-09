package com.aislego.reviews.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ForbiddenException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.domain.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Covers the verified-purchase gate (only a delivered-order customer may review) and the
 * one-review-per-(user, supermarket) upsert behaviour - the two rules the DB schema can't
 * enforce on its own.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long USER_ID = 42L;
    private static final Long SUPERMARKET_ID = 7L;

    private Supermarket supermarket;
    private User user;

    @BeforeEach
    void setUp() {
        supermarket = new Supermarket();
        supermarket.setId(SUPERMARKET_ID);
        supermarket.setName("ValueMart");

        user = new User();
        user.setId(USER_ID);
        user.setFullName("Test Customer");

        lenient().when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(supermarket));
    }

    @Test
    void submitReviewCreatesANewReviewWhenTheCustomerHasADeliveredOrder() {
        when(orderRepository.existsByUserIdAndSupermarketIdAndStatus(USER_ID, SUPERMARKET_ID, OrderStatus.DELIVERED))
                .thenReturn(true);
        when(reviewRepository.findByUserIdAndSupermarketId(USER_ID, SUPERMARKET_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.submitReview(USER_ID, SUPERMARKET_ID, new SubmitReviewRequest(5, "Great store"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great store");
        assertThat(response.reviewerName()).isEqualTo("Test Customer");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        org.mockito.Mockito.verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getSupermarket()).isEqualTo(supermarket);
    }

    @Test
    void submitReviewEditsTheExistingRowInsteadOfCreatingADuplicate() {
        Review existing = new Review();
        existing.setId(99L);
        existing.setUser(user);
        existing.setSupermarket(supermarket);
        existing.setRating(2);
        existing.setComment("Meh");

        when(orderRepository.existsByUserIdAndSupermarketIdAndStatus(USER_ID, SUPERMARKET_ID, OrderStatus.DELIVERED))
                .thenReturn(true);
        when(reviewRepository.findByUserIdAndSupermarketId(USER_ID, SUPERMARKET_ID)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.submitReview(USER_ID, SUPERMARKET_ID, new SubmitReviewRequest(4, "Better now"));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Better now");
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).getReferenceById(any());
    }

    @Test
    void submitReviewIsRejectedWithoutADeliveredOrder() {
        when(orderRepository.existsByUserIdAndSupermarketIdAndStatus(USER_ID, SUPERMARKET_ID, OrderStatus.DELIVERED))
                .thenReturn(false);

        assertThatThrownBy(() -> reviewService.submitReview(USER_ID, SUPERMARKET_ID, new SubmitReviewRequest(5, "Nice")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("delivered");

        org.mockito.Mockito.verify(reviewRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void submitReviewThrowsNotFoundForAnUnknownSupermarket() {
        when(supermarketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview(USER_ID, 999L, new SubmitReviewRequest(5, "Nice")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMyReviewStatusReflectsNoOrderAndNoReviewYet() {
        when(orderRepository.existsByUserIdAndSupermarketIdAndStatus(USER_ID, SUPERMARKET_ID, OrderStatus.DELIVERED))
                .thenReturn(false);
        when(reviewRepository.findByUserIdAndSupermarketId(USER_ID, SUPERMARKET_ID)).thenReturn(Optional.empty());

        MyReviewStatusResponse status = reviewService.getMyReviewStatus(USER_ID, SUPERMARKET_ID);

        assertThat(status.eligible()).isFalse();
        assertThat(status.myReview()).isNull();
    }

    @Test
    void getStoreReviewsReturnsNullAverageWhenThereAreNoReviews() {
        when(reviewRepository.findBySupermarketIdOrderByCreatedAtDesc(SUPERMARKET_ID)).thenReturn(List.of());
        when(reviewRepository.summarizeRatings(List.of(SUPERMARKET_ID))).thenReturn(List.of());

        StoreReviewsResponse response = reviewService.getStoreReviews(SUPERMARKET_ID);

        assertThat(response.averageRating()).isNull();
        assertThat(response.reviewCount()).isZero();
        assertThat(response.reviews()).isEmpty();
    }

    @Test
    void getStoreReviewsReturnsTheAggregateAndTheReviewList() {
        Review review = new Review();
        review.setId(1L);
        review.setUser(user);
        review.setSupermarket(supermarket);
        review.setRating(4);
        when(reviewRepository.findBySupermarketIdOrderByCreatedAtDesc(SUPERMARKET_ID)).thenReturn(List.of(review));

        RatingSummaryView summary = new RatingSummaryView() {
            @Override
            public Long getSupermarketId() {
                return SUPERMARKET_ID;
            }

            @Override
            public Double getAverageRating() {
                return 4.0;
            }

            @Override
            public Long getReviewCount() {
                return 1L;
            }
        };
        when(reviewRepository.summarizeRatings(List.of(SUPERMARKET_ID))).thenReturn(List.of(summary));

        StoreReviewsResponse response = reviewService.getStoreReviews(SUPERMARKET_ID);

        assertThat(response.averageRating()).isEqualTo(4.0);
        assertThat(response.reviewCount()).isEqualTo(1L);
        assertThat(response.reviews()).hasSize(1);
    }
}
