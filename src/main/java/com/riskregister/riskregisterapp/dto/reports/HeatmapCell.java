package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

import com.riskregister.riskregisterapp.entities.Risk;

/** One cell of a 5x5 likelihood x impact heatmap, carrying the risks that land in it. */
public record HeatmapCell(
    int likelihood,
    int impact,
    int score,
    String level,        // Very Low / Low / Medium / High / Very High
    String cellClass,    // Tailwind background+text classes for the cell
    List<Risk> risks
) {
    public int count() {
        return risks.size();
    }

    public boolean isEmpty() {
        return risks.isEmpty();
    }

    public String impactLabel() {
        return Risk.valueToLabel(impact);
    }

    public String likelihoodLabel() {
        return Risk.valueToLabel(likelihood);
    }
}
