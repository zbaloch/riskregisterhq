package com.riskregister.riskregisterapp.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.AuditTrail;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {

    // Primary query: all entries for a given entity, newest first
    List<AuditTrail> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType, Long entityId);

    // Organization-scoped query
    List<AuditTrail> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        Long organizationId, String entityType, Long entityId);
}
