package com.aislego.orders.service;

import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.money.Money;
import com.aislego.identity.repository.UserRepository;
import com.aislego.orders.domain.Cart;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.repository.CartItemRepository;
import com.aislego.orders.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Covers the platform's one hard business rule: a cart can only contain products from a
 * single supermarket.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private static final Long USER_ID = 5L;

    private Cart existingCart;

    @BeforeEach
    void setUp() {
        existingCart = new Cart();
        existingCart.setId(100L);
        existingCart.setSupermarketId(1L);
    }

    @Test
    void rejectsAddingProductFromADifferentSupermarket() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingCart));

        Product otherStoreProduct = productFrom(2L, 50L, "Milk");
        when(productRepository.findById(50L)).thenReturn(Optional.of(otherStoreProduct));

        AddCartItemRequest request = new AddCartItemRequest(50L, 1);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("single supermarket");

        // the cart must not have been mutated/saved as a result of the rejected add
        org.mockito.Mockito.verify(cartRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void allowsAddingProductFromTheSameSupermarketAlreadyPinnedToTheCart() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingCart));

        Product sameStoreProduct = productFrom(1L, 51L, "Bread");
        when(productRepository.findById(51L)).thenReturn(Optional.of(sameStoreProduct));
        when(cartItemRepository.findByCartIdAndProductId(100L, 51L)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.addItem(USER_ID, new AddCartItemRequest(51L, 2));

        assertThat(response.supermarketId()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void firstItemAddedPinsTheCartsSupermarket() {
        Cart emptyCart = new Cart();
        emptyCart.setId(200L);
        emptyCart.setSupermarketId(null);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyCart));

        Product product = productFrom(7L, 60L, "Rice");
        when(productRepository.findById(60L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(anyLong(), any())).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.addItem(USER_ID, new AddCartItemRequest(60L, 1));

        assertThat(response.supermarketId()).isEqualTo(7L);
    }

    private Product productFrom(Long supermarketId, Long productId, String name) {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(supermarketId);
        supermarket.setName("Store " + supermarketId);

        Product product = new Product();
        product.setId(productId);
        product.setSupermarket(supermarket);
        product.setName(name);
        product.setSku("SKU-" + productId);
        product.setPrice(Money.of(BigDecimal.valueOf(50), "INR"));
        return product;
    }
}
