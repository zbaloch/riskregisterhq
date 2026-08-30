package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskCategory;

public interface RiskCategoryRepository extends JpaRepository<RiskCategory, Long> {
    List<RiskCategory> findAllByOrderByNameAsc();
    Optional<RiskCategory> findTopByOrderByIdDesc();
}
