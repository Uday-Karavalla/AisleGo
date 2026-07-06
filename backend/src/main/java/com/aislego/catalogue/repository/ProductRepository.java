package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Browse/search a single store's catalogue. Uses Postgres {@code ILIKE} (accelerated by
     * the pg_trgm GIN index in the V1 migration) as an honest stand-in for a real search
     * engine like OpenSearch - fine for one store's product count, not meant to scale to
     * platform-wide full-text search.
     */
    @Query(value = """
            SELECT p.* FROM products p
            LEFT JOIN categories c ON c.id = p.category_id
            WHERE p.supermarket_id = :supermarketId
              AND p.active = true
              AND (:search IS NULL OR p.name ILIKE CONCAT('%', :search, '%'))
              AND (:category IS NULL OR c.name = :category)
            ORDER BY p.name ASC
            """,
            countQuery = """
            SELECT count(*) FROM products p
            LEFT JOIN categories c ON c.id = p.category_id
            WHERE p.supermarket_id = :supermarketId
              AND p.active = true
              AND (:search IS NULL OR p.name ILIKE CONCAT('%', :search, '%'))
              AND (:category IS NULL OR c.name = :category)
            """,
            nativeQuery = true)
    Page<Product> searchInSupermarket(@Param("supermarketId") Long supermarketId,
                                       @Param("search") String search,
                                       @Param("category") String category,
                                       Pageable pageable);

    @Query(value = """
            SELECT DISTINCT c.name FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE p.supermarket_id = :supermarketId AND p.active = true
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<String> findDistinctCategoryNamesBySupermarketId(@Param("supermarketId") Long supermarketId);
}
