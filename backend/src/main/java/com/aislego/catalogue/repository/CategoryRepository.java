package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
