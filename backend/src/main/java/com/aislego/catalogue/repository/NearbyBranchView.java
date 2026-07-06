package com.aislego.catalogue.repository;

/**
 * Interface-backed projection for the bounding-box "nearby branch candidates" query in
 * {@link BranchRepository}. Column aliases in the SQL must match these getter names. Carries no
 * distance/duration - those are computed by {@code RoutingService} in Java from the raw
 * coordinates, once for the whole candidate batch, rather than per-row in SQL.
 */
public interface NearbyBranchView {
    Long getId();

    String getName();

    String getAddressLine();

    String getCity();

    Double getLatitude();

    Double getLongitude();

    Long getSupermarketId();

    String getSupermarketName();

    String getOpeningTime();

    String getClosingTime();
}
