package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // Organization-scoped, soft-delete aware
    List<Issue> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long organizationId);
    Optional<Issue> findByOrganizationIdAndIdAndDeletedAtIsNull(Long organizationId, Long id);
    long countByOrganizationIdAndDeletedAtIsNull(Long organizationId);

    // Highest existing reference, used to allocate the next ISS-nnn
    Optional<Issue> findTopByOrganizationIdOrderByIdDesc(Long organizationId);

    // Reference uniqueness. Soft-deleted issues are excluded so a removed reference is reusable.
    Optional<Issue> findByOrganizationIdAndIssueRefIgnoreCaseAndDeletedAtIsNull(
        Long organizationId, String issueRef);
}
