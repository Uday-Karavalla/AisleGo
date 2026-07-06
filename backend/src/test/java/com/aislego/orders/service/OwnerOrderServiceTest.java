package com.aislego.orders.service;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
import com.aislego.inventory.service.InventoryReservationService;
import com.aislego.inventory.service.StockReservationLine;
import com.aislego.notifications.Notification;
import com.aislego.notifications.NotificationService;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderItem;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.OwnerOrderResponse;
import com.aislego.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the two hard rules of owner order management: an owner can only ever see/advance
 * orders belonging to their own supermarket, and only the explicitly allowed forward
 * transitions (see {@link OwnerOrderService#advanceStatus}) are permitted.
 */
@ExtendWith(MockitoExtension.class)
class OwnerOrderServiceTest {

    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryReservationService inventoryReservationService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OwnerOrderService service;

    private static final Long OWNER_ID = 5L;
    private static final Long MY_SUPERMARKET_ID = 10L;
    private static final Long ORDER_ID = 100L;

    private Supermarket mySupermarket;

    @BeforeEach
    void setUp() {
        mySupermarket = new Supermarket();
        mySupermarket.setId(MY_SUPERMARKET_ID);
        mySupermarket.setName("My Store");
    }

    @Test
    void listOrdersReturnsOnlyTheCallersSupermarketsOrders() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Order order = buildOrder(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findBySupermarketIdOrderByCreatedAtDesc(MY_SUPERMARKET_ID)).thenReturn(List.of(order));

        List<OwnerOrderResponse> result = service.listOrders(OWNER_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerName()).isEqualTo("Jane Customer");
        verify(orderRepository, never()).findBySupermarketIdAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listOrdersFiltersByStatusWhenGiven() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Order order = buildOrder(OrderStatus.PICKING);
        when(orderRepository.findBySupermarketIdAndStatusOrderByCreatedAtDesc(MY_SUPERMARKET_ID, OrderStatus.PICKING))
                .thenReturn(List.of(order));

        List<OwnerOrderResponse> result = service.listOrders(OWNER_ID, OrderStatus.PICKING);

        assertThat(result).hasSize(1);
        verify(orderRepository, never()).findBySupermarketIdOrderByCreatedAtDesc(any());
    }

    @Test
    void advanceStatusMovesAnAllowedTransitionForwardAndNotifiesTheCustomer() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Order order = buildOrder(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findByIdAndSupermarketId(ORDER_ID, MY_SUPERMARKET_ID)).thenReturn(Optional.of(order));

        OwnerOrderResponse response = service.advanceStatus(OWNER_ID, ORDER_ID, OrderStatus.ACCEPTED_BY_STORE);

        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED_BY_STORE);
        verify(orderRepository).save(order);
        verify(notificationService).send(any(Notification.class));
        verify(inventoryReservationService, never()).release(any(), any());
    }

    @Test
    void advanceStatusRejectsATransitionThatSkipsStages() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Order order = buildOrder(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findByIdAndSupermarketId(ORDER_ID, MY_SUPERMARKET_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.advanceStatus(OWNER_ID, ORDER_ID, OrderStatus.PACKING))
                .isInstanceOf(ConflictException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void advanceStatusToCancelledReleasesReservedStock() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Order order = buildOrder(OrderStatus.PICKING);
        when(orderRepository.findByIdAndSupermarketId(ORDER_ID, MY_SUPERMARKET_ID)).thenReturn(Optional.of(order));

        service.advanceStatus(OWNER_ID, ORDER_ID, OrderStatus.CANCELLED);

        ArgumentCaptor<List<StockReservationLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryReservationService).release(any(), linesCaptor.capture());
        assertThat(linesCaptor.getValue()).hasSize(1);
    }

    @Test
    void advanceStatusThrowsNotFoundWhenTheOrderBelongsToAnotherSupermarket() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        when(orderRepository.findByIdAndSupermarketId(ORDER_ID, MY_SUPERMARKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.advanceStatus(OWNER_ID, ORDER_ID, OrderStatus.ACCEPTED_BY_STORE))
                .isInstanceOf(NotFoundException.class);
    }

    private Order buildOrder(OrderStatus status) {
        User user = new User();
        user.setId(1L);
        user.setFullName("Jane Customer");
        user.setEmail("jane@example.com");
        user.setPhone("+15551234567");

        Branch branch = new Branch();
        branch.setId(3L);
        branch.setName("Main Branch");

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUser(user);
        order.setSupermarket(mySupermarket);
        order.setBranch(branch);
        order.setStatus(status);
        order.setTotalAmount(Money.of(BigDecimal.valueOf(120), "INR"));

        com.aislego.catalogue.domain.Product catalogueProduct = new com.aislego.catalogue.domain.Product();
        catalogueProduct.setId(50L);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(catalogueProduct);
        orderItem.setProductName("Milk");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(Money.of(BigDecimal.valueOf(60), "INR"));
        orderItem.setLineTotal(Money.of(BigDecimal.valueOf(120), "INR"));
        order.getItems().add(orderItem);

        return order;
    }
}
