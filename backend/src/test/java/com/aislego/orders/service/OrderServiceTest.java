package com.aislego.orders.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 9L;
    private static final Long ORDER_ID = 100L;

    @Test
    void getMyOrderStatusReturnsTheOrdersCurrentStatus() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderStatus status = orderService.getMyOrderStatus(USER_ID, ORDER_ID);

        assertThat(status).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
    }

    @Test
    void getMyOrderStatusThrowsNotFoundWhenTheOrderDoesNotBelongToTheUser() {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrderStatus(USER_ID, ORDER_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
