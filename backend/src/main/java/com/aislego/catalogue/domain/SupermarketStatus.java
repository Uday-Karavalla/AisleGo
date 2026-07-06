package com.aislego.catalogue.domain;

/**
 * Verification state of a self-registered {@link Supermarket}. A supermarket only appears in
 * customer-facing discovery once {@link #VERIFIED} - see {@code StoreDiscoveryService} and the
 * {@code BranchRepository} nearby-stores query. Pre-existing (V2 seed) supermarkets are
 * grandfathered in as {@link #VERIFIED} by the V3 migration.
 */
public enum SupermarketStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
