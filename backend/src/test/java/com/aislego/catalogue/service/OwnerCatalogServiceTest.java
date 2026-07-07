package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Category;
import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.dto.CreateBranchRequest;
import com.aislego.catalogue.dto.CreateProductRequest;
import com.aislego.catalogue.dto.OwnerProductResponse;
import com.aislego.catalogue.dto.UpdateInventoryRequest;
import com.aislego.catalogue.dto.UpdateProductRequest;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.catalogue.repository.CategoryRepository;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ForbiddenException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
import com.aislego.inventory.domain.Inventory;
import com.aislego.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the one hard rule of owner self-service catalogue management: an owner can only
 * ever see or modify their own supermarket's branches/products, regardless of what id they
 * pass in - see {@link OwnerCatalogService#findOwnedBranch} / {@code findOwnedProduct}.
 */
@ExtendWith(MockitoExtension.class)
class OwnerCatalogServiceTest {

    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private OwnerCatalogService service;

    private static final Long OWNER_ID = 5L;
    private static final Long MY_SUPERMARKET_ID = 10L;
    private static final Long OTHER_SUPERMARKET_ID = 99L;

    private Supermarket mySupermarket;

    @BeforeEach
    void setUp() {
        User verifiedOwner = new User();
        verifiedOwner.setId(OWNER_ID);
        verifiedOwner.setEmailVerified(true);

        mySupermarket = new Supermarket();
        mySupermarket.setId(MY_SUPERMARKET_ID);
        mySupermarket.setName("My Store");
        mySupermarket.setOwner(verifiedOwner);
    }

    @Test
    void createBranchRejectsAnUnverifiedOwner() {
        mySupermarket.getOwner().setEmailVerified(false);
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));

        assertThatThrownBy(() -> service.createBranch(OWNER_ID,
                new CreateBranchRequest("Main Branch", "12 Market Road", "Springfield", 12.9, 77.6, "09:00", "21:00")))
                .isInstanceOf(ForbiddenException.class);
        verify(branchRepository, never()).save(any());
    }

    @Test
    void createBranchSavesABranchOwnedByTheCallersSupermarket() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        when(branchRepository.save(any())).thenAnswer(inv -> {
            Branch b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        var response = service.createBranch(OWNER_ID,
                new CreateBranchRequest("Main Branch", "12 Market Road", "Springfield", 12.9, 77.6, "09:00", "21:00"));

        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepository).save(captor.capture());
        assertThat(captor.getValue().getSupermarket()).isEqualTo(mySupermarket);
        assertThat(response.name()).isEqualTo("Main Branch");
    }

    @Test
    void createProductRejectsABranchBelongingToAnotherSupermarket() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Branch foreignBranch = buildBranch(1L, OTHER_SUPERMARKET_ID);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(foreignBranch));

        CreateProductRequest request = new CreateProductRequest(
                "Milk", "desc", "SKU-1", BigDecimal.TEN, "INR", "Dairy", null, 1L, 10);

        assertThatThrownBy(() -> service.createProduct(OWNER_ID, request))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProductSavesProductAndInitialInventoryReusingAnExistingCategory() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Branch myBranch = buildBranch(1L, MY_SUPERMARKET_ID);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(myBranch));

        Category dairy = new Category();
        dairy.setId(2L);
        dairy.setName("Dairy");
        when(categoryRepository.findByName("Dairy")).thenReturn(Optional.of(dairy));

        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
        when(inventoryRepository.findByProductId(7L)).thenReturn(List.of());

        CreateProductRequest request = new CreateProductRequest(
                "Milk", "Fresh milk", "SKU-1", new BigDecimal("58.00"), "INR", "Dairy", null, 1L, 25);

        OwnerProductResponse response = service.createProduct(OWNER_ID, request);

        assertThat(response.name()).isEqualTo("Milk");
        assertThat(response.categoryName()).isEqualTo("Dairy");
        verify(categoryRepository, never()).save(any());

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getQuantityOnHand()).isEqualTo(25);
        assertThat(inventoryCaptor.getValue().getBranch()).isEqualTo(myBranch);
    }

    @Test
    void createProductCreatesANewCategoryWhenItDoesNotExistYet() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Branch myBranch = buildBranch(1L, MY_SUPERMARKET_ID);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(myBranch));
        when(categoryRepository.findByName("Bakery")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(3L);
            return c;
        });
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });
        when(inventoryRepository.findByProductId(8L)).thenReturn(List.of());

        CreateProductRequest request = new CreateProductRequest(
                "Bread", null, "SKU-2", new BigDecimal("40.00"), "INR", "Bakery", null, 1L, 5);

        OwnerProductResponse response = service.createProduct(OWNER_ID, request);

        assertThat(response.categoryName()).isEqualTo("Bakery");
        verify(categoryRepository).save(any());
    }

    @Test
    void updateProductRejectsAProductBelongingToAnotherSupermarket() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Product foreignProduct = buildProduct(1L, OTHER_SUPERMARKET_ID);
        when(productRepository.findById(1L)).thenReturn(Optional.of(foreignProduct));

        UpdateProductRequest request = new UpdateProductRequest(
                "New name", null, BigDecimal.TEN, "INR", null, null, true);

        assertThatThrownBy(() -> service.updateProduct(OWNER_ID, 1L, request))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateInventoryCreatesANewRowWhenNoneExistsForThatBranch() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Product myProduct = buildProduct(7L, MY_SUPERMARKET_ID);
        when(productRepository.findById(7L)).thenReturn(Optional.of(myProduct));
        Branch myBranch = buildBranch(1L, MY_SUPERMARKET_ID);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(myBranch));
        when(inventoryRepository.findByBranchIdAndProductId(1L, 7L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(7L)).thenReturn(List.of());

        service.updateInventory(OWNER_ID, 7L, new UpdateInventoryRequest(1L, 40));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityOnHand()).isEqualTo(40);
    }

    @Test
    void updateInventoryUpdatesTheExistingRowWhenOneAlreadyExists() {
        when(supermarketRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(mySupermarket));
        Product myProduct = buildProduct(7L, MY_SUPERMARKET_ID);
        when(productRepository.findById(7L)).thenReturn(Optional.of(myProduct));
        Branch myBranch = buildBranch(1L, MY_SUPERMARKET_ID);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(myBranch));

        Inventory existing = new Inventory();
        existing.setId(50L);
        existing.setBranch(myBranch);
        existing.setProduct(myProduct);
        existing.setQuantityOnHand(5);
        when(inventoryRepository.findByBranchIdAndProductId(1L, 7L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProductId(7L)).thenReturn(List.of(existing));

        service.updateInventory(OWNER_ID, 7L, new UpdateInventoryRequest(1L, 99));

        assertThat(existing.getQuantityOnHand()).isEqualTo(99);
        verify(inventoryRepository).save(existing);
    }

    private Branch buildBranch(Long id, Long supermarketId) {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(supermarketId);
        Branch branch = new Branch();
        branch.setId(id);
        branch.setSupermarket(supermarket);
        branch.setName("Branch " + id);
        return branch;
    }

    private Product buildProduct(Long id, Long supermarketId) {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(supermarketId);
        Product product = new Product();
        product.setId(id);
        product.setSupermarket(supermarket);
        product.setName("Product " + id);
        product.setSku("SKU-" + id);
        product.setPrice(Money.of(BigDecimal.TEN, "INR"));
        return product;
    }
}
