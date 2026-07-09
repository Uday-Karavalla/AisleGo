package com.aislego.reviews.domain;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.common.entity.BaseEntity;
import com.aislego.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A customer's 1-5 star rating (with an optional written comment) of one supermarket.
 * Gated in {@code ReviewService}, not here, on the reviewer having a delivered order from
 * that supermarket - this table has no way to see order history at insert time. One row per
 * (user, supermarket): a repeat review edits this row rather than adding a new one.
 */
@Getter
@Setter
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "supermarket_id"}))
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private Supermarket supermarket;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;
}
