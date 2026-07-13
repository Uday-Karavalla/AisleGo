package com.aislego.growth.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.growth.dto.FavoritesResponse;
import com.aislego.growth.service.FavoritesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavoritesController {
    private final FavoritesService favoritesService;

    public FavoritesController(FavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    @GetMapping
    public FavoritesResponse list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return favoritesService.list(principal.userId());
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<Void> addProduct(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @PathVariable Long productId) {
        favoritesService.addProduct(principal.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> removeProduct(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @PathVariable Long productId) {
        favoritesService.removeProduct(principal.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stores/{supermarketId}")
    public ResponseEntity<Void> addStore(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable Long supermarketId) {
        favoritesService.addStore(principal.userId(), supermarketId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/stores/{supermarketId}")
    public ResponseEntity<Void> removeStore(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable Long supermarketId) {
        favoritesService.removeStore(principal.userId(), supermarketId);
        return ResponseEntity.noContent().build();
    }
}
