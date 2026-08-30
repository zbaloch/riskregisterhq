-- Flyway V6: admin-managed dropdown options (Settings → Managed Fields).
--
-- One table serves every managed field; the lookup_type column names which one. Adding a
-- new managed field therefore needs no schema change — only a new LookupType constant and
-- its seed rows.
--
-- Records store lookup_values.code, not the id or the display name, so an administrator can
-- rename an option without rewriting historical records. issues.source already holds the
-- old enum names (INTERNAL_AUDIT, ...) and those are seeded as the codes, so existing issue
-- rows keep resolving with no data migration.
CREATE TABLE lookup_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lookup_type VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    active BIT(1) DEFAULT b'1',
    flag_value BIT(1) DEFAULT b'0',
    system_default BIT(1) DEFAULT b'0',
    organization_id BIGINT,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lookup_values_type ON lookup_values (organization_id, lookup_type);
CREATE UNIQUE INDEX uq_lookup_values_code ON lookup_values (organization_id, lookup_type, code);
