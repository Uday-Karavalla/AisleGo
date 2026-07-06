package com.aislego.orders.domain;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.common.entity.BaseEntity;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}
