package com.aislego.identity.domain;

/**
 * All platform roles. Only {@link #CUSTOMER} is fully wired up for the first working flow
 * (registration/login and the shopping journey); the rest exist so the schema and JWT
 * claims are stable as later phases (store-side ops, delivery, admin) come online.
 */
public enum Role {
    CUSTOMER,
    SUPERMARKET_OWNER,
    BRANCH_MANAGER,
    PICKER,
    DELIVERY_PARTNER,
    ADMIN
}
