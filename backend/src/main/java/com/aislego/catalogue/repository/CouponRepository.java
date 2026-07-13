package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCodeIgnoreCaseAndSupermarketId(String code, Long supermarketId);

    boolean existsByCodeIgnoreCaseAndSupermarketIsNull(String code);

    Optional<Coupon> findByCodeIgnoreCaseAndSupermarketId(String code, Long supermarketId);

    Optional<Coupon> findByCodeIgnoreCaseAndSupermarketIsNull(String code);

    List<Coupon> findBySupermarketIdOrderByCreatedAtDesc(Long supermarketId);

    List<Coupon> findBySupermarketIsNullOrderByCreatedAtDesc();
}
