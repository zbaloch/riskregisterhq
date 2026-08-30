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
    long countByRiskCategoryIdAndDeletedAtIsNull(Long categoryId);
    long countByStatusIdAndRiskCategoryIdAndDeletedAtIsNull(Long statusId, Long categoryId);

    // Risk ID allocation and clash detection. Returns a list rather than an Optional because
    // registers created before this check could already contain duplicates.
    List<Risk> findByOrganizationIdAndRiskIdIgnoreCaseAndDeletedAtIsNull(Long organizationId, String riskId);

    // Organization-scoped queries
    List<Risk> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId);
    Optional<Risk> findByOrganizationIdAndIdAndDeletedAtIsNull(Long organizationId, Long id);
    long countByOrganizationIdAndDeletedAtIsNull(Long organizationId);
    long countByOrganizationIdAndStatusIdAndDeletedAtIsNull(Long organizationId, Long statusId);
    long countByOrganizationIdAndRiskCategoryIdAndDeletedAtIsNull(Long organizationId, Long categoryId);
    long countByOrganizationIdAndStatusIdAndRiskCategoryIdAndDeletedAtIsNull(Long organizationId, Long statusId, Long categoryId);

    /*
     * Taxonomy counts come in two flavours, and they are not interchangeable:
     *
     *  - What the admin page DISPLAYS: active risks belonging to the current organisation.
     *    This must match what the user sees in the register, or a deleted risk keeps
     *    inflating the count long after it left the list.
     *
     *  - What BLOCKS a delete: every risk anywhere that still references the lookup id,
     *    soft-deleted ones included. risk_categories has no organization_id, so the
     *    taxonomy is shared — deleting an entry one organisation still references would
     *    orphan its records.
     */

    // Delete protection: any reference at all, across organisations, including soft-deleted
    long countByRiskCategoryId(Long categoryId);
    long countByRiskSubcategoryId(Long subcategoryId);

    // Displayed counts: this organisation's active risks only
    @Query("""
        select r.riskCategoryId, count(r) from Risk r
        where r.organizationId = :organizationId
          and r.riskCategoryId is not null
          and r.deletedAt is null
        group by r.riskCategoryId
        """)
    List<Object[]> countActiveRisksGroupedByCategory(@Param("organizationId") Long organizationId);

    @Query("""
        select r.riskSubcategoryId, count(r) from Risk r
        where r.organizationId = :organizationId
          and r.riskSubcategoryId is not null
          and r.deletedAt is null
        group by r.riskSubcategoryId
        """)
    List<Object[]> countActiveRisksGroupedBySubcategory(@Param("organizationId") Long organizationId);

    @Query("select r.riskSubcategoryId, r.riskCategoryId, count(r) from Risk r " +
           "where r.riskSubcategoryId is not null and r.riskCategoryId is not null " +
           "group by r.riskSubcategoryId, r.riskCategoryId")
    List<Object[]> countRisksGroupedBySubcategoryAndCategory();
}
