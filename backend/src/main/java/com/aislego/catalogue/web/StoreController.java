package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.CategoriesResponse;
import com.aislego.catalogue.dto.NearbyBranchResponse;
import com.aislego.catalogue.dto.SupermarketResponse;
import com.aislego.catalogue.service.ProductCatalogueService;
import com.aislego.catalogue.service.StoreDiscoveryService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreDiscoveryService storeDiscoveryService;
    private final ProductCatalogueService productCatalogueService;

    public StoreController(StoreDiscoveryService storeDiscoveryService,
                            ProductCatalogueService productCatalogueService) {
        this.storeDiscoveryService = storeDiscoveryService;
        this.productCatalogueService = productCatalogueService;
    }

    /**
     * Store discovery: nearby branches ordered by distance, via {@code RoutingService} (real
     * driving distance when OpenRouteService is enabled, great-circle estimate otherwise).
     */
    @GetMapping("/nearby")
    public List<NearbyBranchResponse> nearby(@RequestParam @NotNull Double lat,
                                              @RequestParam @NotNull Double lng,
                                              @RequestParam(defaultValue = "10") double radiusKm) {
        return storeDiscoveryService.findNearby(lat, lng, radiusKm);
    }

    @GetMapping("/{supermarketId}")
    public SupermarketResponse get(@PathVariable Long supermarketId) {
        return storeDiscoveryService.getSupermarket(supermarketId);
    }

    /** Distinct category names present in one store's catalogue, for the storefront's filter chips. */
    @GetMapping("/{supermarketId}/categories")
    public CategoriesResponse categories(@PathVariable Long supermarketId) {
        return new CategoriesResponse(productCatalogueService.listCategories(supermarketId));
    }
}
