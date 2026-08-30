package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Legacy risk category table. Risk categories are now admin-managed options
 * (LookupType.RISK_CATEGORY); this entity survives only so
 * DataInitializer.migrateRiskCategoriesToLookup() can carry the old names across on upgrade.
 * Once every deployment has migrated, this and the table can go.
 */
@Entity
@Table(name = "risk_categories")
@Getter
@Setter
public class RiskCategory {

    @Id
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
