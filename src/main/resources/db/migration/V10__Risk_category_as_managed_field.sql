-- Flyway V10: risk category becomes an admin-managed field; subcategory is retired.
--
-- Risks now hold a lookup_values.code for LookupType.RISK_CATEGORY, matching how the issue
-- register works, so a category can be renamed without rewriting history.
--
-- This migration is deliberately ADDITIVE ONLY. The data move (seeding the lookup options
-- from risk_categories, then filling risks.risk_category from risk_category_id) runs at
-- application startup in DataInitializer.migrateRiskCategoriesToLookup(), because that code
-- also has to work in development, where Flyway is disabled and Hibernate manages the schema.
--
-- The old columns and tables are left in place on purpose:
--   risks.risk_category_id, risks.risk_subcategory_id, risk_categories, risk_subcategories
-- They stop being read once this release is live. Dropping them is a separate migration, to
-- be applied only after the backfill has been confirmed in production — a failed backfill
-- with the source data already gone would be unrecoverable.
ALTER TABLE risks ADD COLUMN risk_category VARCHAR(64) NULL;

CREATE INDEX idx_risks_risk_category ON risks (risk_category);
