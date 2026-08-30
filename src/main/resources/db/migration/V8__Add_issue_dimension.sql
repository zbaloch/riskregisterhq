-- Flyway V8: impact area (dimension) on issues.
--
-- Holds a lookup_values.code for LookupType.ISSUE_DIMENSION, seeded with the same names as
-- risk_dimensions so exposure lines up across the two registers on day one.
--
-- Held on the issue rather than inherited from a linked risk: an issue may have no linked
-- risk at all, may link to several with differing dimensions, and the harm from a deficiency
-- is not always the harm from the risk it undermines.
--
-- Nullable at the database level because issues raised before this column existed have no
-- impact area; the form requires one, so those records acquire it on first edit.
ALTER TABLE issues ADD COLUMN dimension VARCHAR(64) NULL;

CREATE INDEX idx_issues_dimension ON issues (dimension);
