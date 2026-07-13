package com.aislego.admin.web;

import com.aislego.catalogue.dto.CouponResponse;
import com.aislego.catalogue.dto.CreateCouponRequest;
import com.aislego.catalogue.dto.UpdateCouponRequest;
import com.aislego.catalogue.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only management of platform-wide coupons (no {@code supermarket}) - see
 *  {@link CouponService}. Store-scoped coupons are managed by owners themselves via
 *  {@link com.aislego.catalogue.web.SupermarketOwnerController}. */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public List<CouponResponse> list() {
        return couponService.listPlatformCoupons();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.createPlatformCoupon(request);
    }

    @PatchMapping("/{couponId}")
    public CouponResponse update(@PathVariable Long couponId, @Valid @RequestBody UpdateCouponRequest request) {
        return couponService.updatePlatformCoupon(couponId, request);
    }

    @DeleteMapping("/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long couponId) {
        couponService.deletePlatformCoupon(couponId);
    }
}
