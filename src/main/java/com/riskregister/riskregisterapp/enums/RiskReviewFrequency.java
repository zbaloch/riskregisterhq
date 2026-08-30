package com.riskregister.riskregisterapp.enums;

import java.time.LocalDate;

public enum RiskReviewFrequency {
	WEEKLY("Weekly", 7),
	MONTHLY("Monthly", 30),
	QUARTERLY("Quarterly", 91),
	SEMI_ANNUALLY("Semi-Annually", 182),
	YEARLY("Yearly", 365);

	private final String displayName;
	private final int days;

	RiskReviewFrequency(String displayName, int days) {
		this.displayName = displayName;
		this.days = days;
	}

	public String getDisplayName() {
		return displayName;
	}

	/** Length of one review cycle in days. */
	public int getDays() {
		return days;
	}

	/** The date a review becomes due, counting one cycle from the last review. */
	public LocalDate nextReviewDate(LocalDate lastReviewed) {
		return lastReviewed == null ? null : lastReviewed.plusDays(days);
	}
}
