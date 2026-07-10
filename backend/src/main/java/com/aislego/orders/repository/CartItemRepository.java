package com.aislego.orders.repository;

import com.aislego.orders.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /** Called when a shopkeeper deletes a product - carts don't carry order history, so
     *  silently dropping it from anyone's in-progress cart is fine (there's nothing to
     *  preserve), unlike an {@code OrderItem} reference, which blocks the delete outright. */
    void deleteByProductId(Long productId);
}
