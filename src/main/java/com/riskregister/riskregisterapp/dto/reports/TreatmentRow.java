package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

import com.riskregister.riskregisterapp.entities.Risk;

/** Risks grouped by the treatment decision taken on them. */
public record TreatmentRow(
    String treatment,      // enum name, e.g. AWAITING_ASSESSMENT
    String label,          // display label, e.g. "Awaiting Assessment"
    String badgeClass,     // Tailwind classes for the treatment pill
    List<Risk> risks
) {
    public int count() {
        return risks.size();
    }

    /** Awaiting Assessment is a backlog, not a decision — the UI highlights it. */
    public boolean isBacklog() {
        return "AWAITING_ASSESSMENT".equals(treatment);
    }
}
