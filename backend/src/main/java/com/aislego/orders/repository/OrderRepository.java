package com.aislego.orders.repository;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** Admin-wide order visibility - see {@code AdminOrderService}. */
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /** Owner-scoped order visibility - see {@code OwnerOrderService}. */
    List<Order> findBySupermarketIdOrderByCreatedAtDesc(Long supermarketId);

    List<Order> findBySupermarketIdAndStatusOrderByCreatedAtDesc(Long supermarketId, OrderStatus status);

    Optional<Order> findByIdAndSupermarketId(Long id, Long supermarketId);
}
