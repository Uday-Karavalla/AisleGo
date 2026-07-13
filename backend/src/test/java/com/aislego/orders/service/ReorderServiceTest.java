package com.aislego.orders.service;

import com.aislego.catalogue.domain.Product;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderItem;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.dto.CartResponse;
import com.aislego.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CartService cartService;

    @Test
    void replacesTheCartWithEveryLineFromThePreviousOrder() {
        Product product = new Product(); product.setId(50L);
        OrderItem item = new OrderItem(); item.setProduct(product); item.setQuantity(3);
        Order order = new Order(); order.setId(12L); order.getItems().add(item);
        CartResponse empty = mock(CartResponse.class);
        CartResponse populated = mock(CartResponse.class);
        when(orderRepository.findByIdAndUserId(12L, 7L)).thenReturn(Optional.of(order));
        when(cartService.clearCart(7L)).thenReturn(empty);
        when(cartService.addItem(7L, new AddCartItemRequest(50L, 3))).thenReturn(populated);

        CartResponse result = new ReorderService(orderRepository, cartService).reorder(7L, 12L);

        assertThat(result).isSameAs(populated);
        verify(cartService).clearCart(7L);
        verify(cartService).addItem(7L, new AddCartItemRequest(50L, 3));
    }
}
