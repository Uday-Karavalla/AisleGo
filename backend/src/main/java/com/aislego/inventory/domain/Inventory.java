package com.aislego.inventory.domain;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Product;
import com.aislego.common.entity.BaseEntity;
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
 * Per-branch stock for a product. {@code version} (inherited from {@link BaseEntity})
 * backs optimistic locking so concurrent checkouts racing for the last few units fail
 * cleanly with a conflict instead of over-selling.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "product_id"}))
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;
}
