package com.aislego.orders.repository;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

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

    List<Order> findByStatusAndFulfilmentTypeNotOrderByCreatedAtAsc(
            OrderStatus status, com.aislego.orders.domain.FulfilmentType fulfilmentType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateById(Long id);

    Optional<Order> findFirstByDeliveryPartnerUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId, java.util.Collection<OrderStatus> statuses);

    List<Order> findByDeliveryPartnerUserIdAndStatusOrderByUpdatedAtDesc(Long userId, OrderStatus status);

    /** Verified-purchase gate for {@code ReviewService} - only a customer with a delivered
     *  order from this supermarket may review it. */
    boolean existsByUserIdAndSupermarketIdAndStatus(Long userId, Long supermarketId, OrderStatus status);

    boolean existsByUserIdAndStatusNot(Long userId, OrderStatus status);
}
