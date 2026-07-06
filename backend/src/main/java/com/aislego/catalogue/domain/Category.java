package com.aislego.catalogue.domain;

import com.aislego.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Platform-wide product category (e.g. "Dairy & Eggs"). Managed by administrators and
 * shared across supermarkets so browsing/filtering is consistent store to store.
 */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;
}
