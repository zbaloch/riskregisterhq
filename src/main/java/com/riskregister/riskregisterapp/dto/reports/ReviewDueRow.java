package com.riskregister.riskregisterapp.dto.reports;

import com.riskregister.riskregisterapp.entities.Risk;

/** A risk whose periodic review is overdue or coming up. */
public record ReviewDueRow(
    Risk risk,
    String categoryName,
    String frequencyLabel,
    Long daysOverdue,        // positive = overdue, negative = days remaining, null = never reviewed
    boolean neverReviewed
) {
    public boolean isOverdue() {
        return neverReviewed || (daysOverdue != null && daysOverdue > 0);
    }

    /** Overdue by more than a full quarter is a governance failure, not a slip. */
    public boolean isSeverelyOverdue() {
        return daysOverdue != null && daysOverdue > 90;
    }

    public String dueLabel() {
        if (neverReviewed) return "Never reviewed";
        if (daysOverdue == null) return "—";
        if (daysOverdue > 0) return daysOverdue + " day" + (daysOverdue == 1 ? "" : "s") + " overdue";
        if (daysOverdue == 0) return "Due today";
        long in = -daysOverdue;
        return "Due in " + in + " day" + (in == 1 ? "" : "s");
    }
}
