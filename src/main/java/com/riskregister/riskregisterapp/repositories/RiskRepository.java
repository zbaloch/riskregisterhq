package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.Risk;

@Repository
public interface RiskRepository extends JpaRepository<Risk, Long> {
    List<Risk> findByDeletedAtIsNullOrderByCreatedAtDesc();
    Optional<Risk> findByIdAndDeletedAtIsNull(Long id);
    long countByDeletedAtIsNull();
    long countByStatusIdAndDeletedAtIsNull(Long statusId);
    long countByRiskCategoryIdAndDeletedAtIsNull(Long categoryId);
    long countByStatusIdAndRiskCategoryIdAndDeletedAtIsNull(Long statusId, Long categoryId);

    // Organization-scoped queries
    List<Risk> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId);
    Optional<Risk> findByOrganizationIdAndIdAndDeletedAtIsNull(Long organizationId, Long id);
    long countByOrganizationIdAndDeletedAtIsNull(Long organizationId);
    long countByOrganizationIdAndStatusIdAndDeletedAtIsNull(Long organizationId, Long statusId);
    long countByOrganizationIdAndRiskCategoryIdAndDeletedAtIsNull(Long organizationId, Long categoryId);
    long countByOrganizationIdAndStatusIdAndRiskCategoryIdAndDeletedAtIsNull(Long organizationId, Long statusId, Long categoryId);
}
