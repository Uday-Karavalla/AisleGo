package com.aislego.inventory.service;

/**
 * A (productId, quantity) line to reserve. Kept independent of the {@code orders} module's
 * CartItem/OrderItem entities so {@code inventory} has no upward dependency on {@code orders}.
 */
public record StockReservationLine(Long productId, int quantity) {
}
