
package com.riskregister.riskregisterapp.entities;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.riskregister.riskregisterapp.enums.RiskReviewFrequency;
import com.riskregister.riskregisterapp.lookups.RiskTreatment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "risks")
@Getter
@Setter
public class Risk {

    private static final Logger log = LoggerFactory.getLogger(Risk.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String riskId; // User-friendly identifier e.g. "RISK-001"

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String riskOwnerName;

    private Long riskCategoryId;
    private Long riskSubcategoryId;
    private Long riskDimensionId;

    private String categories; // Comma-separated list of category tags

    private String linkedAssetIds; // Comma-separated list of linked asset IDs

    @Enumerated(EnumType.STRING)
    private RiskReviewFrequency reviewFrequency;

    // Inherent risk - 1=Very Low, 2=Low, 3=Medium, 4=High, 5=Very High
    private Integer inherentLikelihood;
    private Integer inherentImpact;

    @Column(columnDefinition = "TEXT")
    private String inherentRationale; // Rationale behind the inherent likelihood and impact scores

    // Residual risk (after controls/measures applied)
    private Integer residualLikelihood;
    private Integer residualImpact;

    @Column(columnDefinition = "TEXT")
    private String residualRationale; // Rationale behind the residual likelihood and impact scores

    @Enumerated(EnumType.STRING)
    private RiskTreatment riskTreatment;

    private Long statusId;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt; // null = active; non-null = soft-deleted

    private String createdByEmail;
    private String updatedByEmail;

    // --- Computed helpers (not persisted) ---

    public Integer getInherentScore() {
        return (inherentLikelihood != null && inherentImpact != null)
                ? inherentLikelihood * inherentImpact : null;
    }

    public Integer getResidualScore() {
        return (residualLikelihood != null && residualImpact != null)
                ? residualLikelihood * residualImpact : null;
    }

    public String getInherentScoreLevel() {
        return scoreToLevel(getInherentScore());
    }

    public String getResidualScoreLevel() {
        String residualScoreLeve = scoreToLevel(getResidualScore());
        return residualScoreLeve;
    }

    private static String scoreToLevel(Integer score) {
        if (score == null) return "Unknown";
        if (score <= 2)  return "Very Low";
        if (score <= 4)  return "Low";
        if (score <= 9) return "Medium";
        if (score <= 16) return "High";
        return "Very High";
    }

    public static String valueToLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Very Low";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            case 5 -> "Very High";
            default -> String.valueOf(v);
        };
    }

    // Instance label helpers – callable from Thymeleaf as properties
    public String getInherentLikelihoodLabel() { return valueToLabel(inherentLikelihood); }
    public String getInherentImpactLabel()      { return valueToLabel(inherentImpact); }
    public String getResidualLikelihoodLabel()  { return valueToLabel(residualLikelihood); }
    public String getResidualImpactLabel()      { return valueToLabel(residualImpact); }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    public String getCreatedAtFormatted()  { return createdAt  != null ? DISPLAY_FMT.format(createdAt)  : null; }
    public String getUpdatedAtFormatted()  { return updatedAt  != null ? DISPLAY_FMT.format(updatedAt)  : null; }
}
