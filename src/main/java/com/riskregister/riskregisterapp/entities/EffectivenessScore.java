package com.riskregister.riskregisterapp.entities;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "effectiveness_scores")
@Getter
@Setter
public class EffectivenessScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double score; // Effectiveness percentage (0-100)
    private Long totalInherentScore; // Sum of all inherent scores
    private Long totalResidualScore; // Sum of all residual scores
    private Integer riskCount; // Number of risks included in calculation

    private Instant calculatedAt; // When this score was calculated

    private Long organizationId;

    public EffectivenessScore() {}

    public EffectivenessScore(Double score, Long totalInherentScore, Long totalResidualScore, Integer riskCount) {
        this.score = score;
        this.totalInherentScore = totalInherentScore;
        this.totalResidualScore = totalResidualScore;
        this.riskCount = riskCount;
        this.calculatedAt = Instant.now();
    }

    public EffectivenessScore(Double score, Long totalInherentScore, Long totalResidualScore, Integer riskCount, Long organizationId) {
        this(score, totalInherentScore, totalResidualScore, riskCount);
        this.organizationId = organizationId;
    }

    public String getScoreLevel() {
        if (score == null) return "Unknown";
        if (score >= 80) return "Excellent";
        if (score >= 50) return "Good";
        return "Needs Improvement";
    }

    public String getScoreLevelColor() {
        if (score == null) return "gray";
        if (score >= 80) return "green";
        if (score >= 50) return "yellow";
        return "red";
    }
}
