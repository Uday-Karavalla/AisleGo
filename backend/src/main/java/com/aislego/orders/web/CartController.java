package com.aislego.orders.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.dto.CartResponse;
import com.aislego.orders.dto.UpdateCartItemRequest;
import com.aislego.orders.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse view(@AuthenticationPrincipal AuthenticatedUser principal) {
        return cartService.viewCart(principal.userId());
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                 @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(principal.userId(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long itemId,
                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(principal.userId(), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long itemId) {
        return cartService.removeItem(principal.userId(), itemId);
    }

    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal AuthenticatedUser principal) {
        return cartService.clearCart(principal.userId());
    }
}
