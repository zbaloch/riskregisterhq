package com.riskregister.riskregisterapp.dto.reports;

/** One month on the effectiveness trend, paired with the exposure behind it. */
public record TrendPoint(
    String label,          // "Mar 2026"
    Double score,          // effectiveness %
    Long totalInherent,
    Long totalResidual,
    Integer riskCount
) {
    public String scoreLabel() {
        return score == null ? "—" : String.format("%.1f%%", score);
    }
}
