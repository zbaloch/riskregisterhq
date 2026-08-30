-- Flyway V9: discussion thread on issues.
--
-- Separate from audit_trails on purpose: the trail records what changed about a finding,
-- this records the conversation around it. Organisation-scoped, unlike the older risk_notes
-- table, so comments cannot leak across tenants.
CREATE TABLE issue_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    organization_id BIGINT,
    content TEXT,
    author_id VARCHAR(255),
    author_name VARCHAR(255),
    created_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_issue_notes_issue ON issue_notes (organization_id, issue_id, created_at);
