-- Flyway V1: Initial Schema
-- Creates all tables for RiskRegisterHQ application

-- Users table (holds authentication and profile info)
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    first_name VARCHAR(255) DEFAULT '',
    last_name VARCHAR(255) DEFAULT '',
    password VARCHAR(255),
    role VARCHAR(255),
    approved TINYINT(1),
    token VARCHAR(255),
    token_expiration_date DATETIME,
    token_used_date DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risk Statuses lookup table (Identified, Assessed, Mitigated, Accepted, Closed)
CREATE TABLE risk_statuses (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risk Categories lookup table
CREATE TABLE risk_categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risk Subcategories lookup table
CREATE TABLE risk_subcategories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risk Dimensions lookup table (Financial, Customer, Opportunity, etc.)
CREATE TABLE risk_dimensions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risks table (main risk register)
CREATE TABLE risks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id VARCHAR(255),
    title VARCHAR(255),
    description TEXT,
    risk_owner_name VARCHAR(255),
    risk_category_id BIGINT,
    risk_subcategory_id BIGINT,
    risk_dimension_id BIGINT,
    categories VARCHAR(255),
    linked_asset_ids VARCHAR(255),
    review_frequency VARCHAR(255),
    inherent_likelihood INT,
    inherent_impact INT,
    inherent_rationale TEXT,
    residual_likelihood INT,
    residual_impact INT,
    residual_rationale TEXT,
    risk_treatment VARCHAR(255),
    status_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    deleted_at DATETIME(6),
    created_by_email VARCHAR(255),
    updated_by_email VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tasks table (mitigation tasks linked to risks)
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(255),
    priority VARCHAR(255),
    assignee_id VARCHAR(255),
    assignee_name VARCHAR(255),
    due_date DATE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    deleted_at DATETIME(6),
    created_by_email VARCHAR(255),
    updated_by_email VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Task Updates table (comments/updates on tasks)
CREATE TABLE task_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    content TEXT,
    author_id VARCHAR(255),
    author_name VARCHAR(255),
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Risk Notes table (notes/comments on risks)
CREATE TABLE risk_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id BIGINT NOT NULL,
    content TEXT,
    author_id VARCHAR(255),
    author_name VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit Trails table (tracks changes to risks and other entities)
CREATE TABLE audit_trails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(255),
    entity_id BIGINT,
    action VARCHAR(255),
    summary TEXT,
    changes_json TEXT,
    actor_email VARCHAR(255),
    actor_name VARCHAR(255),
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Assets table (IT assets, data, facilities, etc.)
CREATE TABLE assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    notes TEXT,
    owner_email VARCHAR(255),
    owner_name VARCHAR(255),
    confidentiality INT NOT NULL,
    integrity INT NOT NULL,
    availability INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    deleted_at DATETIME(6),
    created_by_email VARCHAR(255),
    updated_by_email VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Asset Notes table (notes/comments on assets)
CREATE TABLE asset_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    created_at DATETIME(6),
    CONSTRAINT fk_asset_notes_asset FOREIGN KEY (asset_id) REFERENCES assets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Effectiveness Scores table (monthly risk management effectiveness)
CREATE TABLE effectiveness_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    score DOUBLE,
    total_inherent_score BIGINT,
    total_residual_score BIGINT,
    risk_count INT,
    calculated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for common queries
CREATE INDEX idx_risks_deleted_at ON risks(deleted_at);
CREATE INDEX idx_risks_status_id ON risks(status_id);
CREATE INDEX idx_tasks_risk_id ON tasks(risk_id);
CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at);
CREATE INDEX idx_risk_notes_risk_id ON risk_notes(risk_id);
CREATE INDEX idx_audit_trails_entity ON audit_trails(entity_type, entity_id);
CREATE INDEX idx_assets_deleted_at ON assets(deleted_at);
CREATE INDEX idx_asset_notes_asset_id ON asset_notes(asset_id);
