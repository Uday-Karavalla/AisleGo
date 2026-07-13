package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.ProductResponse;
import com.aislego.catalogue.service.ProductCatalogueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores/{supermarketId}/products")
public class ProductController {

    private final ProductCatalogueService productCatalogueService;

    public ProductController(ProductCatalogueService productCatalogueService) {
        this.productCatalogueService = productCatalogueService;
    }

    /** Browse/search a single store's catalogue - paginated, optional free-text search and/or category filter. */
    @GetMapping
    public Page<ProductResponse> browse(@PathVariable Long supermarketId,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String category,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return productCatalogueService.browse(supermarketId, search, category, pageable);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable Long supermarketId, @PathVariable Long productId) {
        return productCatalogueService.get(supermarketId, productId);
    }
}
