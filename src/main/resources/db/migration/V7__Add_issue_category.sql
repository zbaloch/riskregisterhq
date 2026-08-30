-- Flyway V7: required category on issues.
--
-- Holds a lookup_values.code for LookupType.ISSUE_CATEGORY, matching how issues.source works.
-- Nullable at the database level because issues raised before this column existed have no
-- category; the form requires one, so those records acquire a category the first time they
-- are edited. Nothing is invented on their behalf.
ALTER TABLE issues ADD COLUMN category VARCHAR(64) NULL;

CREATE INDEX idx_issues_category ON issues (category);
