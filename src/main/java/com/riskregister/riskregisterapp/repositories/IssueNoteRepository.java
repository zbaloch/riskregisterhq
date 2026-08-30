package com.riskregister.riskregisterapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.IssueNote;

@Repository
public interface IssueNoteRepository extends JpaRepository<IssueNote, Long> {

    List<IssueNote> findByOrganizationIdAndIssueIdOrderByCreatedAtAsc(Long organizationId, Long issueId);

    Optional<IssueNote> findByOrganizationIdAndId(Long organizationId, Long id);

    long countByOrganizationIdAndIssueId(Long organizationId, Long issueId);
}
