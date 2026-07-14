package com.aislego.orders.domain;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.common.entity.BaseEntity;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
import com.aislego.delivery.domain.DeliveryPartnerProfile;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An order is always for exactly one supermarket ({@link #supermarket}) - there is no
 * list of supermarkets here by design, which is what makes cross-store orders structurally
 * impossible once an order exists. The {@code (user_id, idempotency_key)} unique
 * constraint (see V1 migration) makes checkout safe to retry.
 */
@Getter
@Setter
@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idempotency_key"}))
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private Supermarket supermarket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_partner_id")
    private DeliveryPartnerProfile deliveryPartner;

    @Column(name = "pickup_otp_hash", length = 100)
    private String pickupOtpHash;

    @Column(name = "delivery_otp_hash", length = 100)
    private String deliveryOtpHash;

    @Column(name = "pickup_otp_expires_at")
    private Instant pickupOtpExpiresAt;

    @Column(name = "delivery_otp_expires_at")
    private Instant deliveryOtpExpiresAt;

    @Column(name = "pickup_otp_attempts", nullable = false)
    private int pickupOtpAttempts;

    @Column(name = "delivery_otp_attempts", nullable = false)
    private int deliveryOtpAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "total_currency", length = 3, nullable = false))
    })
    private Money totalAmount;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    /** Formatted snapshot of the delivery address at checkout time - deliberately not a live
     *  join to {@code Address}, so editing or deleting a saved address later never changes
     *  where an already-placed order says it's going. Null for pickup orders / no address
     *  selected. */
    @Column(name = "delivery_address")
    private String deliveryAddress;

    /** Snapshot of the coupon code applied at checkout, if any - not a foreign key, same
     *  reasoning as {@link #deliveryAddress}: this order's history shouldn't change if the
     *  coupon is later edited or deleted. Null if no coupon was applied. */
    @Column(name = "coupon_code")
    private String couponCode;

    /** In {@link #totalAmount}'s currency; zero (not null) when no coupon was applied, so
     *  callers never need a null check to display it. */
    @Column(name = "discount_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;

    /** Delivery-fee snapshot charged for this order. Keeping it on the order makes payment
     *  retries and historical price breakdowns independent of future pricing-rule changes. */
    @Column(name = "delivery_fee", precision = 19, scale = 2, nullable = false)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfilment_type", nullable = false)
    private FulfilmentType fulfilmentType = FulfilmentType.IMMEDIATE;

    /** Requested delivery time for scheduled orders; null for ASAP delivery and pickup. */
    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}
