package com.riskregister.riskregisterapp.enums;

/**
 * Issue lifecycle. Unlike a risk, an issue has a terminal state — it closes and stays closed.
 * REMEDIATED is deliberately distinct from CLOSED: the fix is done but nobody independent has
 * confirmed it works yet, and that gap is the whole point of issue validation.
 */
public enum IssueStatus {
    OPEN("Open", false),
    IN_REMEDIATION("In Remediation", false),
    REMEDIATED("Remediated – Awaiting Validation", false),
    CLOSED("Closed", true),
    RISK_ACCEPTED("Risk Accepted", true);

    private final String displayName;
    private final boolean terminal;

    IssueStatus(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Terminal states stop the overdue clock. */
    public boolean isTerminal() {
        return terminal;
    }
}
