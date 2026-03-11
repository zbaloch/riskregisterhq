package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.Id;

public class RiskMeasure {
    @Id
    private Long id;
    private String title;
    private String description; // This is the description of the risk measure, e.g.,
    private String category;
    private Risk risk; // This is the risk that is being measured, linked to the risk table.
}
