package com.aislego.catalogue.repository;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupermarketRepository extends JpaRepository<Supermarket, Long> {

    List<Supermarket> findByStatus(SupermarketStatus status);

    Optional<Supermarket> findByOwnerId(Long ownerId);
}
