package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "risk_subcategories")
@Getter
@Setter
public class RiskSubcategory {

    @Id
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
