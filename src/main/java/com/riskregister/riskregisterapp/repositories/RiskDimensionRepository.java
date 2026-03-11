package com.riskregister.riskregisterapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskDimension;

public interface RiskDimensionRepository extends JpaRepository<RiskDimension, Long> {
}
