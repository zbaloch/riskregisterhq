-- Flyway V4: Periodic review tracking on risks + configurable risk appetite per organisation.
--
-- risks.last_reviewed_at powers the "Reviews Due" report: combined with review_frequency it
-- yields a due date. Existing rows are backfilled at application startup
-- (DataInitializer.backfillLastReviewedAt) from updated_at/created_at, since an edit is the
-- best available evidence the risk was looked at.
ALTER TABLE risks ADD COLUMN last_reviewed_at DATETIME NULL;
ALTER TABLE risks ADD COLUMN last_reviewed_by_name VARCHAR(255) NULL;

CREATE INDEX idx_risks_last_reviewed_at ON risks (last_reviewed_at);

-- Residual score at/above which a risk exceeds appetite and needs sign-off.
-- Null falls back to 15 in code (Organization.getEffectiveRiskAppetiteThreshold).
ALTER TABLE organizations ADD COLUMN risk_appetite_threshold INT NULL;

UPDATE organizations SET risk_appetite_threshold = 15 WHERE risk_appetite_threshold IS NULL;
