package com.riskregister.riskregisterapp.dto.reports;

import java.time.LocalDate;
import java.util.List;

/** "What changed in this period" — the report auditors ask for. */
public record ChangeReport(
    LocalDate from,
    LocalDate to,
    long risksCreated,
    long risksDeleted,
    long risksUpdated,
    long scoreChanges,
    long tasksTouched,
    long taxonomyChanges,
    List<ChangeEntry> entries
) {
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
