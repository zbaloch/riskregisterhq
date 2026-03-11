package com.riskregister.riskregisterapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskSubcategory;

public interface RiskSubcategoryRepository extends JpaRepository<RiskSubcategory, Long> {
}
