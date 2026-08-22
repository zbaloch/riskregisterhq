package com.riskregister.riskregisterapp.enums;

public enum RiskReviewFrequency {
	WEEKLY("Weekly"),
	MONTHLY("Monthly"),
	QUARTERLY("Quarterly"),
	SEMI_ANNUALLY("Semi-Annually"),
	YEARLY("Yearly");

	private final String displayName;

	RiskReviewFrequency(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
