package com.riskregister.riskregisterapp.dto.reports;

import java.util.List;

import com.riskregister.riskregisterapp.entities.Issue;
import com.riskregister.riskregisterapp.entities.Risk;

/**
 * A risk together with the findings raised against its controls. Open severe issues are
 * evidence the risk's residual score is optimistic — the controls it credits are not working.
 */
public record IssuesByRiskRow(
    Risk risk,
    String categoryName,
    List<Issue> issues,
    long openIssues,
    long openSevere
) {
    /** The residual score cannot be trusted while severe findings sit open against it. */
    public boolean scoreInDoubt() {
        return openSevere > 0;
    }

    public int totalIssues() {
        return issues.size();
    }
}
