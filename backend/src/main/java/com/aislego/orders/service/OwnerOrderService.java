package com.aislego.orders.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.domain.User;
import com.aislego.inventory.service.InventoryReservationService;
import com.aislego.inventory.service.StockReservationLine;
import com.aislego.notifications.Notification;
import com.aislego.notifications.NotificationService;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.OwnerOrderResponse;
import com.aislego.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets a supermarket owner see and drive the orders placed at their own store through
 * fulfilment - the piece that was missing entirely before: order status could only ever
 * move PLACED -> PAYMENT_CONFIRMED automatically (via the payment gateway) or -> CANCELLED
 * on payment failure; nothing progressed it any further. This is the human/store-ops
 * counterpart of that automatic step.
 *
 * <p>The allowed transitions deliberately skip the not-yet-built stages
 * ({@code SUBSTITUTION_APPROVAL}, {@code DELIVERY_PARTNER_ASSIGNED}, {@code PICKED_UP},
 * {@code OUT_FOR_DELIVERY}) - with no delivery-partner module, the store is the one
 * fulfilling the whole order, so it goes straight from {@code READY_FOR_PICKUP} to
 * {@code DELIVERED} once handed over. {@code CANCELLED} is reachable from any non-terminal
 * state so a store can back out of an order it can't fulfil (releasing its reserved stock).
 */
@Service
@Transactional
public class OwnerOrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PAYMENT_CONFIRMED, Set.of(OrderStatus.ACCEPTED_BY_STORE, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED_BY_STORE, Set.of(OrderStatus.PICKING, OrderStatus.CANCELLED),
            OrderStatus.PICKING, Set.of(OrderStatus.PACKING, OrderStatus.CANCELLED),
            OrderStatus.PACKING, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED),
            OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
    );

    private final SupermarketRepository supermarketRepository;
    private final OrderRepository orderRepository;
    private final InventoryReservationService inventoryReservationService;
    private final NotificationService notificationService;

    public OwnerOrderService(SupermarketRepository supermarketRepository, OrderRepository orderRepository,
                              InventoryReservationService inventoryReservationService,
                              NotificationService notificationService) {
        this.supermarketRepository = supermarketRepository;
        this.orderRepository = orderRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<OwnerOrderResponse> listOrders(Long ownerId, OrderStatus statusFilter) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        List<Order> orders = statusFilter == null
                ? orderRepository.findBySupermarketIdOrderByCreatedAtDesc(supermarket.getId())
                : orderRepository.findBySupermarketIdAndStatusOrderByCreatedAtDesc(supermarket.getId(), statusFilter);
        return orders.stream().map(OwnerOrderResponse::from).toList();
    }

    public OwnerOrderResponse advanceStatus(Long ownerId, Long orderId, OrderStatus newStatus) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        Order order = orderRepository.findByIdAndSupermarketId(orderId, supermarket.getId())
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new ConflictException("INVALID_TRANSITION",
                    "Cannot move an order from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            inventoryReservationService.release(order.getBranch().getId(), toReservationLines(order));
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        notifyCustomer(order, newStatus);
        return OwnerOrderResponse.from(order);
    }

    private void notifyCustomer(Order order, OrderStatus newStatus) {
        String message = switch (newStatus) {
            case ACCEPTED_BY_STORE -> "Your order #" + order.getId() + " has been accepted by the store.";
            case PICKING -> "Your order #" + order.getId() + " is being picked.";
            case PACKING -> "Your order #" + order.getId() + " is being packed.";
            case READY_FOR_PICKUP -> "Your order #" + order.getId() + " is ready for pickup.";
            case DELIVERED -> "Your order #" + order.getId() + " has been delivered.";
            case CANCELLED -> "Your order #" + order.getId() + " was cancelled by the store.";
            default -> "Your order #" + order.getId() + " is now " + newStatus + ".";
        };
        User user = order.getUser();
        notificationService.send(new Notification(user.getFullName(), user.getEmail(), user.getPhone(),
                "Order update", message));
    }

    private List<StockReservationLine> toReservationLines(Order order) {
        return order.getItems().stream()
                .map(item -> new StockReservationLine(item.getProduct().getId(), item.getQuantity()))
                .toList();
    }

    private Supermarket getOwnedSupermarket(Long ownerId) {
        return supermarketRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No supermarket is registered to this account"));
    }
}
