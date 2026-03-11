package com.riskregister.riskregisterapp.repositories;

import com.riskregister.riskregisterapp.entities.RiskNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskNoteRepository extends JpaRepository<RiskNote, Long> {
    List<RiskNote> findByRiskIdOrderByCreatedAtAsc(Long riskId);
}
