package com.aislego.growth.repository;

import com.aislego.growth.domain.GrowthEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthEventRepository extends JpaRepository<GrowthEvent, Long> {
}
