package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.BranchResponse;
import com.aislego.catalogue.dto.CreateBranchRequest;
import com.aislego.catalogue.dto.CreateProductRequest;
import com.aislego.catalogue.dto.MySupermarketResponse;
import com.aislego.catalogue.dto.OwnerProductResponse;
import com.aislego.catalogue.dto.UpdateInventoryRequest;
import com.aislego.catalogue.dto.UpdateProductRequest;
import com.aislego.catalogue.service.OwnerCatalogService;
import com.aislego.catalogue.service.SupermarketVerificationService;
import com.aislego.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/supermarkets")
@PreAuthorize("hasRole('SUPERMARKET_OWNER')")
public class SupermarketOwnerController {

    private final SupermarketVerificationService verificationService;
    private final OwnerCatalogService ownerCatalogService;

    public SupermarketOwnerController(SupermarketVerificationService verificationService,
                                       OwnerCatalogService ownerCatalogService) {
        this.verificationService = verificationService;
        this.ownerCatalogService = ownerCatalogService;
    }

    /**
     * Lets a supermarket owner check their own application's review status
     * (PENDING/VERIFIED/REJECTED) and, if rejected, the reason.
     */
    @GetMapping("/mine")
    public ResponseEntity<MySupermarketResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        var supermarket = verificationService.getMine(principal.userId());
        return ResponseEntity.ok(MySupermarketResponse.from(supermarket));
    }

    @GetMapping("/mine/branches")
    public List<BranchResponse> listBranches(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ownerCatalogService.listBranches(principal.userId());
    }

    @PostMapping("/mine/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse createBranch(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @Valid @RequestBody CreateBranchRequest request) {
        return ownerCatalogService.createBranch(principal.userId(), request);
    }

    @GetMapping("/mine/products")
    public List<OwnerProductResponse> listProducts(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ownerCatalogService.listProducts(principal.userId());
    }

    @PostMapping("/mine/products")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerProductResponse createProduct(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody CreateProductRequest request) {
        return ownerCatalogService.createProduct(principal.userId(), request);
    }

    @PatchMapping("/mine/products/{productId}")
    public OwnerProductResponse updateProduct(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long productId,
                                               @Valid @RequestBody UpdateProductRequest request) {
        return ownerCatalogService.updateProduct(principal.userId(), productId, request);
    }

    @PatchMapping("/mine/products/{productId}/inventory")
    public OwnerProductResponse updateInventory(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long productId,
                                                 @Valid @RequestBody UpdateInventoryRequest request) {
        return ownerCatalogService.updateInventory(principal.userId(), productId, request);
    }
}
