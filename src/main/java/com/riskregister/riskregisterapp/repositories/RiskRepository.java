package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.Risk;

@Repository
public interface RiskRepository extends JpaRepository<Risk, Long> {
    List<Risk> findByDeletedAtIsNullOrderByCreatedAtDesc();
    Optional<Risk> findByIdAndDeletedAtIsNull(Long id);
    long countByDeletedAtIsNull();
    long countByStatusIdAndDeletedAtIsNull(Long statusId);
    long countByRiskCategoryAndDeletedAtIsNull(String riskCategory);

    /**
     * Legacy (risk id, risk_category_id) pairs, for the one-time move to managed categories.
     * Native because Risk no longer maps the old column; throws on databases that never had
     * it, which the caller treats as "nothing to migrate".
     */
    @Query(value = "SELECT id, risk_category_id FROM risks WHERE risk_category_id IS NOT NULL",
           nativeQuery = true)
    List<Object[]> findLegacyCategoryAssignments();

    // Risk ID allocation and clash detection. Returns a list rather than an Optional because
    // registers created before this check could already contain duplicates.
    List<Risk> findByOrganizationIdAndRiskIdIgnoreCaseAndDeletedAtIsNull(Long organizationId, String riskId);

    // Organization-scoped queries
    List<Risk> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId);
    Optional<Risk> findByOrganizationIdAndIdAndDeletedAtIsNull(Long organizationId, Long id);
    long countByOrganizationIdAndDeletedAtIsNull(Long organizationId);
    long countByOrganizationIdAndStatusIdAndDeletedAtIsNull(Long organizationId, Long statusId);
    long countByOrganizationIdAndRiskCategoryAndDeletedAtIsNull(Long organizationId, String riskCategory);
    long countByOrganizationIdAndStatusIdAndRiskCategoryAndDeletedAtIsNull(Long organizationId, Long statusId, String riskCategory);

    /**
     * Risks per category code for this organisation, active only, for the dashboard and
     * reports. Category delete protection now lives with the managed-fields framework
     * (LookupService.usageCounts), which counts soft-deleted risks too.
     */
    @Query("""
        select r.riskCategory, count(r) from Risk r
        where r.organizationId = :organizationId
          and r.riskCategory is not null
          and r.deletedAt is null
        group by r.riskCategory
        """)
    List<Object[]> countActiveRisksGroupedByCategory(@Param("organizationId") Long organizationId);
}
