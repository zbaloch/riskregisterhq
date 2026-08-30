-- Flyway V5: Issue register — control deficiencies, audit findings and regulatory findings.
--
-- Deliberately has NO likelihood column. An issue has already occurred, so its probability
-- is 1.0; it is rated on severity (impact x pervasiveness) instead. Mixing a likelihood
-- score into findings understates them and makes issue and risk scores non-comparable.
CREATE TABLE issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_ref VARCHAR(255),
    title VARCHAR(255),
    description TEXT,
    source VARCHAR(50),
    external_reference VARCHAR(255),

    -- Severity axes, 1-5 each
    impact INT,
    pervasiveness INT,

    root_cause TEXT,
    remediation_plan TEXT,
    owner_name VARCHAR(255),
    status VARCHAR(50),

    date_raised DATE,
    target_date DATE,
    -- Never overwritten once set, so date extensions remain visible
    original_target_date DATE,
    extension_count INT DEFAULT 0,
    closed_date DATE,

    validated_by_name VARCHAR(255),
    validated_at DATETIME NULL,

    linked_risk_ids TEXT,
    linked_asset_ids TEXT,

    organization_id BIGINT,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    created_by_email VARCHAR(255),
    updated_by_email VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_issues_organization_id ON issues (organization_id);
CREATE INDEX idx_issues_status ON issues (status);
CREATE INDEX idx_issues_target_date ON issues (target_date);

-- A task now hangs off a risk OR an issue; when set, the task is a remediation action.
ALTER TABLE tasks ADD COLUMN issue_id BIGINT NULL;

CREATE INDEX idx_tasks_issue_id ON tasks (issue_id);
