package com.aislego.catalogue.web;

import com.aislego.catalogue.dto.BranchResponse;
import com.aislego.catalogue.dto.CreateBranchRequest;
import com.aislego.catalogue.dto.CreateProductRequest;
import com.aislego.catalogue.dto.MySupermarketResponse;
import com.aislego.catalogue.dto.OwnerProductResponse;
import com.aislego.catalogue.dto.UpdateBranchRequest;
import com.aislego.catalogue.dto.UpdateInventoryRequest;
import com.aislego.catalogue.dto.UpdateProductRequest;
import com.aislego.catalogue.service.OwnerCatalogService;
import com.aislego.catalogue.service.SupermarketVerificationService;
import com.aislego.common.security.AuthenticatedUser;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.OwnerOrderResponse;
import com.aislego.orders.dto.UpdateOrderStatusRequest;
import com.aislego.orders.service.OwnerOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/supermarkets")
@PreAuthorize("hasRole('SUPERMARKET_OWNER')")
public class SupermarketOwnerController {

    private final SupermarketVerificationService verificationService;
    private final OwnerCatalogService ownerCatalogService;
    private final OwnerOrderService ownerOrderService;

    public SupermarketOwnerController(SupermarketVerificationService verificationService,
                                       OwnerCatalogService ownerCatalogService,
                                       OwnerOrderService ownerOrderService) {
        this.verificationService = verificationService;
        this.ownerCatalogService = ownerCatalogService;
        this.ownerOrderService = ownerOrderService;
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

    @PatchMapping("/mine/branches/{branchId}")
    public BranchResponse updateBranch(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @PathVariable Long branchId,
                                        @Valid @RequestBody UpdateBranchRequest request) {
        return ownerCatalogService.updateBranch(principal.userId(), branchId, request);
    }

    @DeleteMapping("/mine/branches/{branchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBranch(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long branchId) {
        ownerCatalogService.deleteBranch(principal.userId(), branchId);
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

    @DeleteMapping("/mine/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long productId) {
        ownerCatalogService.deleteProduct(principal.userId(), productId);
    }

    /** Omit {@code status} for every order at this store; pass it to filter to one stage. */
    @GetMapping("/mine/orders")
    public List<OwnerOrderResponse> listOrders(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @RequestParam(required = false) OrderStatus status) {
        return ownerOrderService.listOrders(principal.userId(), status);
    }

    @PatchMapping("/mine/orders/{orderId}/status")
    public OwnerOrderResponse updateOrderStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long orderId,
                                                 @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ownerOrderService.advanceStatus(principal.userId(), orderId, request.status());
    }
}
