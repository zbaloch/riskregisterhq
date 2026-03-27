package com.riskregister.riskregisterapp.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riskregister.riskregisterapp.entities.EffectivenessScore;

@Repository
public interface EffectivenessScoreRepository extends JpaRepository<EffectivenessScore, Long> {
    List<EffectivenessScore> findByCalculatedAtAfterOrderByCalculatedAtAsc(Instant after);
    EffectivenessScore findFirstByOrderByCalculatedAtDesc();

    // Organization-scoped queries
    List<EffectivenessScore> findByOrganizationIdAndCalculatedAtAfterOrderByCalculatedAtAsc(
        Long organizationId, Instant after);
    EffectivenessScore findFirstByOrganizationIdOrderByCalculatedAtDesc(Long organizationId);
}
