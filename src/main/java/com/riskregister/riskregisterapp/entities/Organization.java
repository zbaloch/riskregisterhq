package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Residual score at or above which a risk exceeds the organisation's risk appetite
     * and needs explicit sign-off. Drives the "Above Appetite" report. Default 15.
     */
    private Integer riskAppetiteThreshold;

    private Instant createdAt;
    private Instant updatedAt;

    /** Threshold with the default applied, so callers never deal with null. */
    public int getEffectiveRiskAppetiteThreshold() {
        return riskAppetiteThreshold != null ? riskAppetiteThreshold : 15;
    }
}
