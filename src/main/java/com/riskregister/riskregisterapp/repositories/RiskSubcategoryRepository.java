package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskSubcategory;

public interface RiskSubcategoryRepository extends JpaRepository<RiskSubcategory, Long> {
    List<RiskSubcategory> findAllByOrderByNameAsc();
    List<RiskSubcategory> findByCategoryIdOrderByNameAsc(Long categoryId);
    List<RiskSubcategory> findByCategoryIdIsNullOrderByNameAsc();
    long countByCategoryId(Long categoryId);
    Optional<RiskSubcategory> findTopByOrderByIdDesc();
}
