package com.aislego.catalogue.domain;

import com.aislego.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A physical branch of a supermarket. Inventory and delivery-area logic hang off the
 * branch, not the supermarket, since a chain's stock and pricing can vary per branch.
 * Latitude/longitude back the Haversine "nearby stores" query - no real routing API yet.
 */
@Getter
@Setter
@Entity
@Table(name = "branches")
public class Branch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supermarket_id", nullable = false)
    private Supermarket supermarket;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_line")
    private String addressLine;

    @Column
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "opening_time")
    private String openingTime;

    @Column(name = "closing_time")
    private String closingTime;

    @Column(nullable = false)
    private boolean active = true;
}
