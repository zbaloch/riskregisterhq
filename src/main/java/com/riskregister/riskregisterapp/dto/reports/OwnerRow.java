package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

import com.riskregister.riskregisterapp.entities.Risk;

/** Everything one risk owner is accountable for, for use in review meetings. */
public record OwnerRow(
    String owner,
    List<Risk> risks,
    Integer maxResidual,      // highest residual score they carry, null when none scored
    long aboveAppetite,
    long reviewsOverdue,
    long openTasks,
    long overdueTasks
) {
    public int riskCount() {
        return risks.size();
    }

    public boolean isUnassigned() {
        return owner == null || owner.isBlank();
    }

    public String ownerLabel() {
        return isUnassigned() ? "Unassigned" : owner;
    }
}
