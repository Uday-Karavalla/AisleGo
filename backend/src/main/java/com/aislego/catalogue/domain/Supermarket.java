package com.aislego.catalogue.domain;

import com.aislego.common.entity.BaseEntity;
import com.aislego.identity.domain.User;
import jakarta.persistence.Column;
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

/**
 * A supermarket business onboarded onto the platform. It keeps its own identity, pricing
 * and inventory - AisleGo does not operate dark stores. A supermarket can have multiple
 * {@link Branch branches}.
 */
@Getter
@Setter
@Entity
@Table(name = "supermarkets")
public class Supermarket extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "phone")
    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * The self-registered owner, if any. Nullable since pre-existing (V2 seed) supermarkets
     * have no owner account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupermarketStatus status;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = true)
    private User reviewedBy;
}
