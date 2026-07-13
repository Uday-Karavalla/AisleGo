package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    boolean existsByOrderId(Long orderId);

    @Query("select count(r) from CouponRedemption r where upper(r.couponCode) = upper(:code) " +
            "and ((:storeId is null and r.supermarketId is null) or r.supermarketId = :storeId)")
    long countForCoupon(@Param("code") String code, @Param("storeId") Long storeId);

    @Query("select count(r) from CouponRedemption r where r.user.id = :userId " +
            "and upper(r.couponCode) = upper(:code) " +
            "and ((:storeId is null and r.supermarketId is null) or r.supermarketId = :storeId)")
    long countForUserAndCoupon(@Param("userId") Long userId, @Param("code") String code,
                               @Param("storeId") Long storeId);
}
