package com.aislego.delivery.domain;

import com.aislego.common.entity.BaseEntity;
import com.aislego.identity.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "delivery_partner_profiles")
public class DeliveryPartnerProfile extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPartnerStatus status = DeliveryPartnerStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;
}
