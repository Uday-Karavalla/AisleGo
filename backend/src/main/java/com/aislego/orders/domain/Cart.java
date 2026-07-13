package com.aislego.orders.domain;

import com.aislego.common.entity.BaseEntity;
import com.aislego.identity.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One active cart per customer. {@code supermarketId} starts null and is pinned to
 * whichever supermarket the first item belongs to - see
 * {@link com.aislego.orders.service.CartService#addItem} for where the
 * single-supermarket-per-cart rule is enforced in application code (the V1 migration also
 * adds a DB trigger as defense in depth).
 */
@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "supermarket_id")
    private Long supermarketId;

    /** A code, not a foreign key to a {@code Coupon} row - see the V11 migration comment. */
    @Column(name = "coupon_code")
    private String couponCode;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
