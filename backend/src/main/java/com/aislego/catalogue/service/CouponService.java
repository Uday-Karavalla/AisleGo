package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.DiscountType;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.dto.CouponResponse;
import com.aislego.catalogue.dto.AvailableCouponResponse;
import com.aislego.catalogue.dto.CreateCouponRequest;
import com.aislego.catalogue.dto.UpdateCouponRequest;
import com.aislego.catalogue.repository.CouponRepository;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;

/**
 * Coupon management (admin platform-wide + owner store-scoped, sharing one CRUD
 * implementation) plus the cart-facing lookup/discount-calculation logic
 * {@link com.aislego.orders.service.CartService} and
 * {@link com.aislego.orders.service.CheckoutService} both call.
 *
 * <p>A code is looked up store-scoped first, then platform-wide - a store's own coupon takes
 * precedence over a same-named platform-wide one for that store's shoppers, per
 * {@link #resolveApplicableCoupon}.
 */
@Service
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final SupermarketRepository supermarketRepository;

    public CouponService(CouponRepository couponRepository, SupermarketRepository supermarketRepository) {
        this.couponRepository = couponRepository;
        this.supermarketRepository = supermarketRepository;
    }

    // ---- Admin: platform-wide coupons ----

    public CouponResponse createPlatformCoupon(CreateCouponRequest request) {
        return CouponResponse.from(save(new Coupon(), null, request));
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listPlatformCoupons() {
        return couponRepository.findBySupermarketIsNullOrderByCreatedAtDesc().stream()
                .map(CouponResponse::from)
                .toList();
    }

    public CouponResponse updatePlatformCoupon(Long couponId, UpdateCouponRequest request) {
        return CouponResponse.from(applyUpdate(findPlatformCoupon(couponId), request));
    }

    public void deletePlatformCoupon(Long couponId) {
        couponRepository.delete(findPlatformCoupon(couponId));
    }

    // ---- Owner: store-scoped coupons ----

    public CouponResponse createStoreCoupon(Long ownerId, CreateCouponRequest request) {
        return CouponResponse.from(save(new Coupon(), getOwnedSupermarket(ownerId), request));
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listStoreCoupons(Long ownerId) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        return couponRepository.findBySupermarketIdOrderByCreatedAtDesc(supermarket.getId()).stream()
                .map(CouponResponse::from)
                .toList();
    }

    public CouponResponse updateStoreCoupon(Long ownerId, Long couponId, UpdateCouponRequest request) {
        return CouponResponse.from(applyUpdate(findOwnedCoupon(ownerId, couponId), request));
    }

    public void deleteStoreCoupon(Long ownerId, Long couponId) {
        couponRepository.delete(findOwnedCoupon(ownerId, couponId));
    }

    // ---- Cart/checkout: resolving and pricing a code ----

    /** Throws with a specific reason - used by the "Apply" action, where the shopper should be
     *  told exactly why a code didn't work. */
    @Transactional(readOnly = true)
    public Coupon resolveApplicableCoupon(String code, Long cartSupermarketId) {
        String normalized = code.trim();
        if (cartSupermarketId != null) {
            Optional<Coupon> storeScoped = couponRepository.findByCodeIgnoreCaseAndSupermarketId(normalized, cartSupermarketId);
            if (storeScoped.isPresent()) {
                return validate(storeScoped.get());
            }
        }
        Coupon platformWide = couponRepository.findByCodeIgnoreCaseAndSupermarketIsNull(normalized)
                .orElseThrow(() -> new NotFoundException("This coupon code isn't valid"));
        return validate(platformWide);
    }

    /** Non-throwing variant for the passive "is the coupon still on this cart still good"
     *  re-check on every cart read - see CartService#toResponse. */
    @Transactional(readOnly = true)
    public Optional<Coupon> tryResolveApplicableCoupon(String code, Long cartSupermarketId) {
        try {
            return Optional.of(resolveApplicableCoupon(code, cartSupermarketId));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Never discounts more than the subtotal itself - a customer's total never goes negative. */
    public Money calculateDiscount(Coupon coupon, Money subtotal) {
        if (coupon == null) {
            return Money.zero(subtotal.getCurrencyCode());
        }
        Money raw = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? Money.of(subtotal.getAmount()
                        .multiply(BigDecimal.valueOf(coupon.getPercentOff()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN), subtotal.getCurrencyCode())
                : compatibleFlatAmount(coupon, subtotal);
        return raw.isGreaterThanOrEqualTo(subtotal) ? subtotal : raw;
    }

    /** Returns only currently usable coupons for this cart, with store coupons taking
     *  precedence over a platform coupon that happens to use the same code. */
    @Transactional(readOnly = true)
    public List<AvailableCouponResponse> listApplicableCoupons(Long supermarketId, Money subtotal) {
        if (supermarketId == null || !subtotal.isPositive()) {
            return List.of();
        }

        LinkedHashMap<String, Coupon> byCode = new LinkedHashMap<>();
        couponRepository.findBySupermarketIdOrderByCreatedAtDesc(supermarketId).stream()
                .filter(this::isCurrentlyValid)
                .filter(coupon -> hasCompatibleCurrency(coupon, subtotal))
                .forEach(coupon -> byCode.put(coupon.getCode().toUpperCase(Locale.ROOT), coupon));
        couponRepository.findBySupermarketIsNullOrderByCreatedAtDesc().stream()
                .filter(this::isCurrentlyValid)
                .filter(coupon -> hasCompatibleCurrency(coupon, subtotal))
                .forEach(coupon -> byCode.putIfAbsent(coupon.getCode().toUpperCase(Locale.ROOT), coupon));

        return byCode.values().stream()
                .map(coupon -> AvailableCouponResponse.from(coupon, calculateDiscount(coupon, subtotal)))
                .toList();
    }

    // ---- Shared helpers ----

    private Coupon save(Coupon coupon, Supermarket supermarket, CreateCouponRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        boolean duplicate = supermarket == null
                ? couponRepository.existsByCodeIgnoreCaseAndSupermarketIsNull(normalizedCode)
                : couponRepository.existsByCodeIgnoreCaseAndSupermarketId(normalizedCode, supermarket.getId());
        if (duplicate) {
            throw new ConflictException("COUPON_CODE_EXISTS",
                    "Coupon code " + normalizedCode + " already exists in this scope");
        }

        coupon.setCode(normalizedCode);
        coupon.setSupermarket(supermarket);
        applyDiscountFields(coupon, request.discountType(), request.percentOff(), request.amountOff(), request.currency());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setActive(true);
        return couponRepository.save(coupon);
    }

    private Coupon applyUpdate(Coupon coupon, UpdateCouponRequest request) {
        applyDiscountFields(coupon, request.discountType(), request.percentOff(), request.amountOff(), request.currency());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setActive(request.active());
        return couponRepository.save(coupon);
    }

    private void applyDiscountFields(Coupon coupon, DiscountType type, Integer percentOff, BigDecimal amountOff,
                                      String currency) {
        coupon.setDiscountType(type);
        if (type == DiscountType.PERCENTAGE) {
            if (percentOff == null || percentOff < 1 || percentOff > 100) {
                throw new BadRequestException("Percentage off must be between 1 and 100");
            }
            coupon.setPercentOff(percentOff);
            coupon.setAmountOff(null);
        } else {
            if (amountOff == null || amountOff.signum() <= 0) {
                throw new BadRequestException("Amount off must be a positive number");
            }
            coupon.setAmountOff(Money.of(amountOff, currency == null || currency.isBlank() ? "INR" : currency));
            coupon.setPercentOff(null);
        }
    }

    private Coupon validate(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new BadRequestException("This coupon is no longer active");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This coupon has expired");
        }
        return coupon;
    }

    private boolean isCurrentlyValid(Coupon coupon) {
        return coupon.isActive() && (coupon.getExpiresAt() == null || !coupon.getExpiresAt().isBefore(Instant.now()));
    }

    private boolean hasCompatibleCurrency(Coupon coupon, Money subtotal) {
        return coupon.getDiscountType() == DiscountType.PERCENTAGE
                || coupon.getAmountOff().getCurrencyCode().equals(subtotal.getCurrencyCode());
    }

    private Money compatibleFlatAmount(Coupon coupon, Money subtotal) {
        if (!hasCompatibleCurrency(coupon, subtotal)) {
            throw new BadRequestException("This coupon is not available for the cart currency");
        }
        return coupon.getAmountOff();
    }

    private Coupon findPlatformCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new NotFoundException("Coupon " + couponId + " was not found"));
        if (coupon.getSupermarket() != null) {
            throw new NotFoundException("Coupon " + couponId + " was not found");
        }
        return coupon;
    }

    private Coupon findOwnedCoupon(Long ownerId, Long couponId) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new NotFoundException("Coupon " + couponId + " was not found"));
        if (coupon.getSupermarket() == null || !coupon.getSupermarket().getId().equals(supermarket.getId())) {
            throw new NotFoundException("Coupon " + couponId + " was not found");
        }
        return coupon;
    }

    private Supermarket getOwnedSupermarket(Long ownerId) {
        return supermarketRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No supermarket is registered to this account"));
    }
}
