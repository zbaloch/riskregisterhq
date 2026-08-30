-- Flyway V3: Link risk subcategories to their parent risk category.
-- The column is nullable: existing rows are backfilled at application startup
-- (DataInitializer.backfillSubcategoryParents) based on how risks actually use
-- each subcategory; anything unresolvable stays NULL and is surfaced in the
-- admin UI as "Uncategorized" for manual assignment.
ALTER TABLE risk_subcategories ADD COLUMN category_id BIGINT NULL;

CREATE INDEX idx_risk_subcategories_category_id ON risk_subcategories (category_id);
