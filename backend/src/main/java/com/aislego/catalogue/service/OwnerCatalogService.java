package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Category;
import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.dto.BranchResponse;
import com.aislego.catalogue.dto.BranchStockResponse;
import com.aislego.catalogue.dto.CreateBranchRequest;
import com.aislego.catalogue.dto.CreateProductRequest;
import com.aislego.catalogue.dto.OwnerProductResponse;
import com.aislego.catalogue.dto.UpdateInventoryRequest;
import com.aislego.catalogue.dto.UpdateProductRequest;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.catalogue.repository.CategoryRepository;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.inventory.domain.Inventory;
import com.aislego.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Self-service catalogue management for a supermarket owner: branches, products and
 * per-branch stock. Every method resolves the caller's own supermarket first (via
 * {@link SupermarketRepository#findByOwnerId}) and every branch/product lookup is
 * re-checked against that supermarket's id - an owner can only ever see or modify their
 * own catalogue, never another store's, regardless of what id they pass in.
 *
 * <p>Deliberately allowed even while the supermarket is still {@code PENDING} review: an
 * owner should be able to set up their branch and catalogue while waiting on admin
 * verification, not be blocked until approved (verification only gates customer-facing
 * visibility - see {@link StoreDiscoveryService}).
 */
@Service
@Transactional
public class OwnerCatalogService {

    private final SupermarketRepository supermarketRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public OwnerCatalogService(SupermarketRepository supermarketRepository, BranchRepository branchRepository,
                                ProductRepository productRepository, CategoryRepository categoryRepository,
                                InventoryRepository inventoryRepository) {
        this.supermarketRepository = supermarketRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(Long ownerId) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        return branchRepository.findBySupermarketIdOrderByNameAsc(supermarket.getId()).stream()
                .map(BranchResponse::from)
                .toList();
    }

    public BranchResponse createBranch(Long ownerId, CreateBranchRequest request) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);

        Branch branch = new Branch();
        branch.setSupermarket(supermarket);
        branch.setName(request.name());
        branch.setAddressLine(request.addressLine());
        branch.setCity(request.city());
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setOpeningTime(request.openingTime());
        branch.setClosingTime(request.closingTime());
        branch.setActive(true);

        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional(readOnly = true)
    public List<OwnerProductResponse> listProducts(Long ownerId) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        return productRepository.findBySupermarketIdOrderByNameAsc(supermarket.getId()).stream()
                .map(product -> OwnerProductResponse.from(product, branchStockFor(product.getId())))
                .toList();
    }

    public OwnerProductResponse createProduct(Long ownerId, CreateProductRequest request) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        Branch branch = findOwnedBranch(supermarket, request.branchId());

        Product product = new Product();
        product.setSupermarket(supermarket);
        product.setCategory(resolveCategory(request.categoryName()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setPrice(Money.of(request.price(), request.currency()));
        product.setImageUrl(request.imageUrl());
        product.setActive(true);
        product = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setBranch(branch);
        inventory.setProduct(product);
        inventory.setQuantityOnHand(request.initialStockQuantity());
        inventoryRepository.save(inventory);

        return OwnerProductResponse.from(product, branchStockFor(product.getId()));
    }

    public OwnerProductResponse updateProduct(Long ownerId, Long productId, UpdateProductRequest request) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        Product product = findOwnedProduct(supermarket, productId);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(Money.of(request.price(), request.currency()));
        product.setCategory(resolveCategory(request.categoryName()));
        product.setImageUrl(request.imageUrl());
        product.setActive(request.active());
        product = productRepository.save(product);

        return OwnerProductResponse.from(product, branchStockFor(product.getId()));
    }

    public OwnerProductResponse updateInventory(Long ownerId, Long productId, UpdateInventoryRequest request) {
        Supermarket supermarket = getOwnedSupermarket(ownerId);
        Product product = findOwnedProduct(supermarket, productId);
        Branch branch = findOwnedBranch(supermarket, request.branchId());

        Inventory inventory = inventoryRepository.findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setBranch(branch);
                    created.setProduct(product);
                    return created;
                });
        inventory.setQuantityOnHand(request.quantityOnHand());
        inventoryRepository.save(inventory);

        return OwnerProductResponse.from(product, branchStockFor(product.getId()));
    }

    private List<BranchStockResponse> branchStockFor(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(inventory -> new BranchStockResponse(
                        inventory.getBranch().getId(), inventory.getBranch().getName(), inventory.getQuantityOnHand()))
                .toList();
    }

    private Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String trimmed = categoryName.trim();
        return categoryRepository.findByName(trimmed).orElseGet(() -> {
            Category category = new Category();
            category.setName(trimmed);
            return categoryRepository.save(category);
        });
    }

    private Supermarket getOwnedSupermarket(Long ownerId) {
        return supermarketRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No supermarket is registered to this account"));
    }

    private Branch findOwnedBranch(Supermarket supermarket, Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch " + branchId + " was not found"));
        if (!branch.getSupermarket().getId().equals(supermarket.getId())) {
            throw new NotFoundException("Branch " + branchId + " was not found");
        }
        return branch;
    }

    private Product findOwnedProduct(Supermarket supermarket, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " was not found"));
        if (!product.getSupermarket().getId().equals(supermarket.getId())) {
            throw new NotFoundException("Product " + productId + " was not found");
        }
        return product;
    }
}
