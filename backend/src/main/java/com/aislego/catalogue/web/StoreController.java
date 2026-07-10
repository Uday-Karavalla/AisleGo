package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.BranchDetailResponse;
import com.aislego.catalogue.dto.CategoriesResponse;
import com.aislego.catalogue.dto.CategoryProductResponse;
import com.aislego.catalogue.dto.NearbyBranchResponse;
import com.aislego.catalogue.dto.SupermarketResponse;
import com.aislego.catalogue.service.ProductCatalogueService;
import com.aislego.catalogue.service.StoreDiscoveryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    /** Resolves one branch by id - what the storefront route actually navigates by. See
     *  {@code StoreDiscoveryService#getBranchDetail}. */
    @GetMapping("/branches/{branchId}")
    public BranchDetailResponse getBranch(@PathVariable Long branchId) {
        return storeDiscoveryService.getBranchDetail(branchId);
    }

    /** Distinct category names present in one store's catalogue, for the storefront's filter chips. */
    @GetMapping("/{supermarketId}/categories")
    public CategoriesResponse categories(@PathVariable Long supermarketId) {
        return new CategoriesResponse(productCatalogueService.listCategories(supermarketId));
    }

    /**
     * Cross-store category browse: one category's products mixed across every nearby
     * supermarket, e.g. the home page's "Fresh veggies daily" / "Dairy &amp; more" / "Fresh
     * fruit" tiles. See {@code StoreDiscoveryService#browseCategoryNearby}.
     */
    @GetMapping("/category-products")
    public Page<CategoryProductResponse> categoryProducts(@RequestParam @NotBlank String category,
                                                            @RequestParam @NotNull Double lat,
                                                            @RequestParam @NotNull Double lng,
                                                            @RequestParam(defaultValue = "10") double radiusKm,
                                                            @PageableDefault(size = 20) Pageable pageable) {
        return storeDiscoveryService.browseCategoryNearby(category, lat, lng, radiusKm, pageable);
    }
}
