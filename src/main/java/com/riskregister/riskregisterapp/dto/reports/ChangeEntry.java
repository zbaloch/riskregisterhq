package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

import com.riskregister.riskregisterapp.dto.FieldChange;
import com.riskregister.riskregisterapp.entities.AuditTrail;

/** One audit trail entry with its field changes already parsed for display. */
public record ChangeEntry(
    AuditTrail entry,
    List<FieldChange> changes,
    String entityLabel,     // "Risk" / "Task" / "Risk Category" ...
    String riskId,          // e.g. RISK-014, when the entry points at a risk
    String riskTitle
) {
    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    /** Score movements are what reviewers scan for first. */
    public List<FieldChange> scoreChanges() {
        if (changes == null) return List.of();
        return changes.stream()
            .filter(c -> c.field() != null
                      && (c.field().startsWith("inherent") || c.field().startsWith("residual")))
            .toList();
    }
}
