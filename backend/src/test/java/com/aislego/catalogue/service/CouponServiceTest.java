package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.DiscountType;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.dto.CreateCouponRequest;
import com.aislego.catalogue.dto.UpdateCouponRequest;
import com.aislego.catalogue.repository.CouponRepository;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private static final Long OWNER_ID = 7L;
    private static final Long SUPERMARKET_ID = 11L;

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private SupermarketRepository supermarketRepository;

    private CouponService couponService;
    private Supermarket supermarket;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, supermarketRepository);
        supermarket = new Supermarket();
        supermarket.setId(SUPERMARKET_ID);
        supermarket.setName("Neighbourhood Market");
    }

    @Test
    void createPlatformCouponNormalizesTheCodeAndStoresPercentageTerms() {
        when(couponRepository.save(any())).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            coupon.setId(1L);
            return coupon;
        });

        var response = couponService.createPlatformCoupon(new CreateCouponRequest(
                "  save15  ", DiscountType.PERCENTAGE, 15, null, null, null));

        assertThat(response.code()).isEqualTo("SAVE15");
        assertThat(response.supermarketId()).isNull();
        assertThat(response.percentOff()).isEqualTo(15);
        assertThat(response.amountOff()).isNull();
        assertThat(response.active()).isTrue();
    }

    @Test
    void createStoreCouponScopesAFlatDiscountToTheOwnersSupermarket() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(supermarket));
        when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = couponService.createStoreCoupon(OWNER_ID, new CreateCouponRequest(
                "flat50", DiscountType.FLAT, null, BigDecimal.valueOf(50), null, null));

        assertThat(response.supermarketId()).isEqualTo(SUPERMARKET_ID);
        assertThat(response.amountOff()).isEqualByComparingTo("50.00");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.percentOff()).isNull();
    }

    @Test
    void duplicatePlatformCodeReturnsADeliberateConflict() {
        when(couponRepository.existsByCodeIgnoreCaseAndSupermarketIsNull("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.createPlatformCoupon(new CreateCouponRequest(
                "save10", DiscountType.PERCENTAGE, 10, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SAVE10")
                .hasMessageContaining("already exists");
        verify(couponRepository, never()).save(any());
    }

    @Test
    void duplicateStoreCodeIsCheckedOnlyWithinThatStoreScope() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(supermarket));
        when(couponRepository.existsByCodeIgnoreCaseAndSupermarketId("SAVE10", SUPERMARKET_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> couponService.createStoreCoupon(OWNER_ID, new CreateCouponRequest(
                "save10", DiscountType.PERCENTAGE, 10, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        verify(couponRepository, never()).existsByCodeIgnoreCaseAndSupermarketIsNull(any());
        verify(couponRepository, never()).save(any());
    }

    @Test
    void storeCanCreateTheSameCodeAsAPlatformCoupon() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(supermarket));
        when(couponRepository.existsByCodeIgnoreCaseAndSupermarketId("SAVE10", SUPERMARKET_ID))
                .thenReturn(false);
        when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = couponService.createStoreCoupon(OWNER_ID, new CreateCouponRequest(
                "save10", DiscountType.PERCENTAGE, 10, null, null, null));

        assertThat(response.code()).isEqualTo("SAVE10");
        assertThat(response.supermarketId()).isEqualTo(SUPERMARKET_ID);
        verify(couponRepository, never()).existsByCodeIgnoreCaseAndSupermarketIsNull(any());
    }

    @Test
    void updateStoreCouponCannotReachAnotherStoresCoupon() {
        Supermarket otherStore = new Supermarket();
        otherStore.setId(99L);
        Coupon coupon = percentageCoupon("SAVE10", 10);
        coupon.setId(4L);
        coupon.setSupermarket(otherStore);

        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(supermarket));
        when(couponRepository.findById(4L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.updateStoreCoupon(OWNER_ID, 4L,
                new UpdateCouponRequest(DiscountType.PERCENTAGE, 20, null, null, null, true)))
                .isInstanceOf(NotFoundException.class);
        verify(couponRepository, never()).save(any());
    }

    @Test
    void storeScopedCodeTakesPrecedenceOverTheSamePlatformCode() {
        Coupon storeCoupon = percentageCoupon("SAVE10", 20);
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketId("save10", SUPERMARKET_ID))
                .thenReturn(Optional.of(storeCoupon));

        Coupon resolved = couponService.resolveApplicableCoupon(" save10 ", SUPERMARKET_ID);

        assertThat(resolved).isSameAs(storeCoupon);
        verify(couponRepository, never()).findByCodeIgnoreCaseAndSupermarketIsNull(any());
    }

    @Test
    void platformCodeIsUsedWhenTheStoreHasNoMatchingCode() {
        Coupon platformCoupon = percentageCoupon("WELCOME", 5);
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketId("WELCOME", SUPERMARKET_ID))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketIsNull("WELCOME"))
                .thenReturn(Optional.of(platformCoupon));

        assertThat(couponService.resolveApplicableCoupon("WELCOME", SUPERMARKET_ID))
                .isSameAs(platformCoupon);
    }

    @Test
    void applyingAnInactiveCouponReturnsASpecificReason() {
        Coupon coupon = percentageCoupon("OLD", 10);
        coupon.setActive(false);
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketIsNull("OLD"))
                .thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.resolveApplicableCoupon("OLD", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    void applyingAnExpiredCouponReturnsASpecificReason() {
        Coupon coupon = percentageCoupon("OLD", 10);
        coupon.setExpiresAt(Instant.now().minusSeconds(60));
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketIsNull("OLD"))
                .thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.resolveApplicableCoupon("OLD", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void passiveResolutionTurnsAnInvalidCouponIntoAnEmptyResult() {
        when(couponRepository.findByCodeIgnoreCaseAndSupermarketIsNull("MISSING"))
                .thenReturn(Optional.empty());

        assertThat(couponService.tryResolveApplicableCoupon("MISSING", null)).isEmpty();
    }

    @Test
    void percentageDiscountUsesMoneyRounding() {
        Coupon coupon = percentageCoupon("THIRD", 33);

        Money discount = couponService.calculateDiscount(coupon, Money.of("10.01", "INR"));

        assertThat(discount).isEqualTo(Money.of("3.30", "INR"));
    }

    @Test
    void flatDiscountIsCappedAtTheSubtotal() {
        Coupon coupon = flatCoupon("BIG", "500.00");

        Money discount = couponService.calculateDiscount(coupon, Money.of("125.00", "INR"));

        assertThat(discount).isEqualTo(Money.of("125.00", "INR"));
    }

    @Test
    void availableCouponsExcludeInactiveAndExpiredOffersAndPreferTheStoreCode() {
        Coupon storeSave = percentageCoupon("SAVE", 20);
        storeSave.setSupermarket(supermarket);
        Coupon expired = percentageCoupon("OLD", 50);
        expired.setSupermarket(supermarket);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        Coupon platformSave = percentageCoupon("SAVE", 5);
        Coupon welcome = flatCoupon("WELCOME", "10.00");
        Coupon wrongCurrency = new Coupon();
        wrongCurrency.setCode("DOLLAR10");
        wrongCurrency.setDiscountType(DiscountType.FLAT);
        wrongCurrency.setAmountOff(Money.of("10.00", "USD"));
        wrongCurrency.setActive(true);

        when(couponRepository.findBySupermarketIdOrderByCreatedAtDesc(SUPERMARKET_ID))
                .thenReturn(List.of(storeSave, expired));
        when(couponRepository.findBySupermarketIsNullOrderByCreatedAtDesc())
                .thenReturn(List.of(platformSave, welcome, wrongCurrency));

        var offers = couponService.listApplicableCoupons(SUPERMARKET_ID, Money.of("100.00", "INR"));

        assertThat(offers).extracting(offer -> offer.code()).containsExactly("SAVE", "WELCOME");
        assertThat(offers.get(0).scope()).isEqualTo("STORE");
        assertThat(offers.get(0).estimatedDiscount()).isEqualByComparingTo("20.00");
        assertThat(offers.get(1).scope()).isEqualTo("PLATFORM");
        assertThat(offers.get(1).estimatedDiscount()).isEqualByComparingTo("10.00");
    }

    @Test
    void nullCouponProducesAZeroDiscountInTheCartCurrency() {
        Money discount = couponService.calculateDiscount(null, Money.of("125.00", "USD"));

        assertThat(discount).isEqualTo(Money.zero("USD"));
    }

    @Test
    void invalidDiscountTermsAreRejectedBeforeSaving() {
        assertThatThrownBy(() -> couponService.createPlatformCoupon(new CreateCouponRequest(
                "TOO-MUCH", DiscountType.PERCENTAGE, 101, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 1 and 100");
        verify(couponRepository, never()).save(any());
    }

    private Coupon percentageCoupon(String code, int percentOff) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setPercentOff(percentOff);
        coupon.setActive(true);
        return coupon;
    }

    private Coupon flatCoupon(String code, String amountOff) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(DiscountType.FLAT);
        coupon.setAmountOff(Money.of(amountOff, "INR"));
        coupon.setActive(true);
        return coupon;
    }
}
