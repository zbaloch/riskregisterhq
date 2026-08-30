package com.riskregister.riskregisterapp.dto.reports;

import com.riskregister.riskregisterapp.entities.Risk;

/**
 * A risk whose mitigation story does not hold up: it is marked for treatment but has
 * no tasks behind it, or every task behind it is overdue or stalled.
 */
public record GapRow(
    Risk risk,
    String categoryName,
    long totalTasks,
    long openTasks,
    long overdueTasks,
    String gap,          // short reason label
    String severity      // "critical" | "warning"
) {
    public boolean isCritical() {
        return "critical".equals(severity);
    }
}
