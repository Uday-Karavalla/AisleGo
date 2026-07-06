package com.aislego.addresses.repository;

import com.aislego.addresses.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdAsc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
