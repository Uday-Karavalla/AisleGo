package com.aislego.catalogue.service;

import com.aislego.catalogue.dto.ProductResponse;
import com.aislego.catalogue.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductCatalogueService {

    private final ProductRepository productRepository;

    public ProductCatalogueService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> browse(Long supermarketId, String search, String category, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String normalizedCategory = (category == null || category.isBlank()) ? null : category.trim();
        return productRepository.searchInSupermarket(supermarketId, normalizedSearch, normalizedCategory, pageable)
                .map(ProductResponse::from);
    }

    public List<String> listCategories(Long supermarketId) {
        return productRepository.findDistinctCategoryNamesBySupermarketId(supermarketId);
    }
}
