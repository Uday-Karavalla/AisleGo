package com.aislego.orders.service;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.FulfilmentType;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.AdminOrderResponse;
import com.aislego.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminOrderService service;

    @Test
    void listAllReturnsEveryOrderWhenNoStatusFilterIsGiven() {
        Pageable pageable = Pageable.ofSize(20);
        Order order = buildOrder();
        when(orderRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        Page<AdminOrderResponse> result = service.listAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).customerEmail()).isEqualTo("jane@example.com");
        assertThat(result.getContent().get(0).couponCode()).isEqualTo("SAVE10");
        assertThat(result.getContent().get(0).discountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getContent().get(0).subtotal()).isEqualByComparingTo("120.00");
        assertThat(result.getContent().get(0).deliveryFee()).isEqualByComparingTo("25.00");
        assertThat(result.getContent().get(0).fulfilmentType()).isEqualTo(FulfilmentType.IMMEDIATE);
        verify(orderRepository, never()).findByStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listAllFiltersByStatusWhenGiven() {
        Pageable pageable = Pageable.ofSize(20);
        Order order = buildOrder();
        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PLACED, pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<AdminOrderResponse> result = service.listAll(OrderStatus.PLACED, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    private Order buildOrder() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Jane Customer");
        user.setEmail("jane@example.com");

        Supermarket supermarket = new Supermarket();
        supermarket.setId(2L);
        supermarket.setName("FreshMart");

        Branch branch = new Branch();
        branch.setId(3L);
        branch.setName("FreshMart Indiranagar");

        Order order = new Order();
        order.setId(100L);
        order.setUser(user);
        order.setSupermarket(supermarket);
        order.setBranch(branch);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(Money.of(BigDecimal.valueOf(135), "INR"));
        order.setCouponCode("SAVE10");
        order.setDiscountAmount(BigDecimal.TEN);
        order.setDeliveryFee(BigDecimal.valueOf(25));
        return order;
    }
}
