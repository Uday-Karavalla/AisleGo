package com.aislego.catalogue.domain;

import com.aislego.common.entity.BaseEntity;
import com.aislego.common.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import com.aislego.identity.domain.User;

/**
 * A discount code, either platform-wide ({@code supermarket == null}, created by an admin) or
 * scoped to one store (created by that store's owner). Exactly one of {@code percentOff} /
 * {@code amountOff} is populated, chosen by {@code discountType} - see
 * {@code CouponService#applyDiscountFields}.
 *
 * <p>Deliberately not referenced by a foreign key from {@code Cart}/{@code Order} - they store
 * the code string instead (see V11 migration comment), so deleting a coupon is always safe and
 * never blocked by past usage.
 */
@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private String code;

    /** Null = platform-wide (admin-created); set = scoped to this one store (owner-created). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supermarket_id")
    private Supermarket supermarket;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "percent_off")
    private Integer percentOff;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount_off", precision = 19, scale = 2)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "amount_off_currency", length = 3))
    })
    private Money amountOff;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly;

    /** Null means there is no platform-wide redemption cap. */
    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    /** Null means a shopper may reuse the coupon while it remains otherwise valid. */
    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    /** Optional owner of an automatically issued welcome/referral reward coupon. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;
}
