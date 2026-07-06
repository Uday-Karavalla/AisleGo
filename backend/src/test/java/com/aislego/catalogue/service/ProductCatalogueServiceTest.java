package com.aislego.catalogue.service;

import com.aislego.catalogue.dto.ProductResponse;
import com.aislego.catalogue.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the blank-vs-null normalization {@link ProductCatalogueService#browse} does before
 * delegating to the repository - blank search/category strings from the frontend must reach
 * the (native-query) repository method as {@code null}, not {@code ""}, or the ILIKE/equality
 * filters would never match anything.
 */
@ExtendWith(MockitoExtension.class)
class ProductCatalogueServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCatalogueService service;

    private static final Long SUPERMARKET_ID = 1L;

    @Test
    void browseNormalizesBlankSearchAndCategoryToNull() {
        Pageable pageable = Pageable.ofSize(20);
        Page<com.aislego.catalogue.domain.Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.searchInSupermarket(eq(SUPERMARKET_ID), isNull(), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        Page<ProductResponse> result = service.browse(SUPERMARKET_ID, "   ", "", pageable);

        assertThat(result).isEmpty();
        verify(productRepository).searchInSupermarket(SUPERMARKET_ID, null, null, pageable);
    }

    @Test
    void browsePassesThroughTrimmedSearchAndCategory() {
        Pageable pageable = Pageable.ofSize(20);
        Page<com.aislego.catalogue.domain.Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.searchInSupermarket(SUPERMARKET_ID, "milk", "Dairy & Eggs", pageable))
                .thenReturn(emptyPage);

        service.browse(SUPERMARKET_ID, "  milk  ", "Dairy & Eggs", pageable);

        verify(productRepository).searchInSupermarket(SUPERMARKET_ID, "milk", "Dairy & Eggs", pageable);
    }

    @Test
    void listCategoriesDelegatesToRepository() {
        when(productRepository.findDistinctCategoryNamesBySupermarketId(SUPERMARKET_ID))
                .thenReturn(List.of("Dairy & Eggs", "Fruits & Vegetables"));

        List<String> categories = service.listCategories(SUPERMARKET_ID);

        assertThat(categories).containsExactly("Dairy & Eggs", "Fruits & Vegetables");
    }
}
