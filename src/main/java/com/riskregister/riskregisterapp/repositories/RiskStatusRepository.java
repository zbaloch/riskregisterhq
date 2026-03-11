package com.riskregister.riskregisterapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.riskregister.riskregisterapp.entities.RiskStatus;

public interface RiskStatusRepository extends JpaRepository<RiskStatus, Long> {
}
