package com.riskregister.riskregisterapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskCategory;

public interface RiskCategoryRepository extends JpaRepository<RiskCategory, Long> {
}
