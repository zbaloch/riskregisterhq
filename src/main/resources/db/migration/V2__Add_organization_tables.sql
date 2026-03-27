-- V2: Multi-tenant organization support

CREATE TABLE organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default organization for existing data
INSERT INTO organizations (name, description, created_at, updated_at)
VALUES ('Default Organization', 'Migrated from single-tenant setup', NOW(), NOW());

-- Add organization_id to all data tables (nullable first for safe migration)
ALTER TABLE users ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE risks ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE tasks ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE assets ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE audit_trails ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE effectiveness_scores ADD COLUMN organization_id BIGINT NULL;

-- Backfill all existing rows with the default org (id=1)
UPDATE users SET organization_id = 1 WHERE organization_id IS NULL;
UPDATE risks SET organization_id = 1 WHERE organization_id IS NULL;
UPDATE tasks SET organization_id = 1 WHERE organization_id IS NULL;
UPDATE assets SET organization_id = 1 WHERE organization_id IS NULL;
UPDATE audit_trails SET organization_id = 1 WHERE audit_trails.organization_id IS NULL;
UPDATE effectiveness_scores SET organization_id = 1 WHERE effectiveness_scores.organization_id IS NULL;

-- Add indexes for org-scoped queries
CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_risks_organization_id ON risks(organization_id);
CREATE INDEX idx_tasks_organization_id ON tasks(organization_id);
CREATE INDEX idx_assets_organization_id ON assets(organization_id);
CREATE INDEX idx_audit_trails_organization_id ON audit_trails(organization_id);
CREATE INDEX idx_effectiveness_scores_organization_id ON effectiveness_scores(organization_id);
