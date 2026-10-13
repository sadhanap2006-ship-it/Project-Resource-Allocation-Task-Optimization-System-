-- ==============================================================================
-- SIST CSE Capstone · Project Resource Allocation System
-- Specification Version 1.0 (30 September 2026)
-- 01-schema.sql: 18 Tables, Constraints, Indexes & System Settings
-- Database Engine: MySQL 8.0 (InnoDB, utf8mb4)
-- ==============================================================================

DROP DATABASE IF EXISTS resource_allocation;
CREATE DATABASE resource_allocation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE resource_allocation;


-- Disable foreign key checks for clean drop/create
SET FOREIGN_KEY_CHECKS = 0;

-- Drop in reverse dependency order for clean recreation if executed standalone
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS simulation_change;
DROP TABLE IF EXISTS simulation_scenario;
DROP TABLE IF EXISTS conflict;
DROP TABLE IF EXISTS budget_transaction;
DROP TABLE IF EXISTS unavailability;
DROP TABLE IF EXISTS allocation;
DROP TABLE IF EXISTS milestone_skill_requirement;
DROP TABLE IF EXISTS milestone;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS resource_skill;
DROP TABLE IF EXISTS skill;
DROP TABLE IF EXISTS cloud_resource;
DROP TABLE IF EXISTS hardware_asset;
DROP TABLE IF EXISTS developer_profile;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS settings;

-- Also clean up any legacy tables from previous prototypes if present
DROP TABLE IF EXISTS developers;
DROP TABLE IF EXISTS resource_allocations;
DROP TABLE IF EXISTS task_feedbacks;
DROP TABLE IF EXISTS task_feedback;
DROP TABLE IF EXISTS project_milestones;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS system_notifications;

-- ------------------------------------------------------------------------------
-- 1. settings
-- ------------------------------------------------------------------------------
CREATE TABLE settings (
    setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. resource
-- ------------------------------------------------------------------------------
CREATE TABLE resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    capacity_pct INT NOT NULL,
    cost_rate DECIMAL(12,2) NOT NULL,
    cost_unit VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_resource_type CHECK (type IN ('DEVELOPER','HARDWARE','CLOUD')),
    CONSTRAINT chk_resource_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT chk_resource_capacity CHECK (capacity_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_resource_cost_rate CHECK (cost_rate >= 0),
    CONSTRAINT chk_resource_cost_unit CHECK (cost_unit IN ('HOUR','DAY','UNIT')),
    INDEX idx_resource_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. users
-- ------------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    resource_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_resource_id UNIQUE (resource_id),
    CONSTRAINT chk_users_role CHECK (role IN ('PM','DEVELOPER','ADMIN')),
    CONSTRAINT fk_users_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. developer_profile
-- ------------------------------------------------------------------------------
CREATE TABLE developer_profile (
    resource_id BIGINT NOT NULL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    job_title VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_developer_profile_email UNIQUE (email),
    CONSTRAINT fk_developer_profile_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. hardware_asset
-- ------------------------------------------------------------------------------
CREATE TABLE hardware_asset (
    resource_id BIGINT NOT NULL PRIMARY KEY,
    asset_tag VARCHAR(50) NOT NULL,
    lab_location VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_hardware_asset_tag UNIQUE (asset_tag),
    CONSTRAINT fk_hardware_asset_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. cloud_resource
-- ------------------------------------------------------------------------------
CREATE TABLE cloud_resource (
    resource_id BIGINT NOT NULL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    unit_name VARCHAR(30) NOT NULL,
    daily_quota_units DECIMAL(12,2) NOT NULL,
    monthly_cap DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_cloud_daily_quota CHECK (daily_quota_units > 0),
    CONSTRAINT chk_cloud_monthly_cap CHECK (monthly_cap > 0),
    CONSTRAINT fk_cloud_resource_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 7. skill
-- ------------------------------------------------------------------------------
CREATE TABLE skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    category VARCHAR(60) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_skill_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 8. resource_skill (Link table)
-- ------------------------------------------------------------------------------
CREATE TABLE resource_skill (
    resource_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency INT NOT NULL,
    PRIMARY KEY (resource_id, skill_id),
    CONSTRAINT chk_resource_skill_proficiency CHECK (proficiency BETWEEN 1 AND 3),
    CONSTRAINT fk_resource_skill_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_skill_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 9. project
-- ------------------------------------------------------------------------------
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    pm_user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_budget DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_code UNIQUE (code),
    CONSTRAINT chk_project_status CHECK (status IN ('PLANNED','ACTIVE','ON_HOLD','COMPLETED','ARCHIVED')),
    CONSTRAINT chk_project_total_budget CHECK (total_budget > 0),
    CONSTRAINT chk_project_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_project_pm_user FOREIGN KEY (pm_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 10. milestone
-- ------------------------------------------------------------------------------
CREATE TABLE milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress_pct INT NOT NULL DEFAULT 0,
    estimated_effort_hours DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_milestone_status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','DELAYED','AT_RISK')),
    CONSTRAINT chk_milestone_progress CHECK (progress_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_milestone_effort CHECK (estimated_effort_hours > 0),
    CONSTRAINT chk_milestone_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    INDEX idx_milestone_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 11. milestone_skill_requirement (Link table)
-- ------------------------------------------------------------------------------
CREATE TABLE milestone_skill_requirement (
    milestone_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    min_proficiency INT NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (milestone_id, skill_id),
    CONSTRAINT chk_requirement_proficiency CHECK (min_proficiency BETWEEN 1 AND 3),
    CONSTRAINT fk_requirement_milestone FOREIGN KEY (milestone_id) REFERENCES milestone(id) ON DELETE CASCADE,
    CONSTRAINT fk_requirement_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 12. allocation
-- ------------------------------------------------------------------------------
CREATE TABLE allocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    allocation_pct INT NOT NULL,
    override_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_allocation_pct CHECK (allocation_pct BETWEEN 1 AND 100),
    CONSTRAINT chk_allocation_dates CHECK (end_date >= start_date),
    CONSTRAINT uq_allocation_resource_milestone_start UNIQUE (resource_id, milestone_id, start_date),
    CONSTRAINT fk_allocation_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_milestone FOREIGN KEY (milestone_id) REFERENCES milestone(id) ON DELETE CASCADE,
    INDEX idx_allocation_resource_dates (resource_id, start_date, end_date),
    INDEX idx_allocation_milestone (milestone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 13. unavailability
-- ------------------------------------------------------------------------------
CREATE TABLE unavailability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NULL,
    requested_by BIGINT NOT NULL,
    decided_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_unavailability_type CHECK (type IN ('LEAVE','MAINTENANCE')),
    CONSTRAINT chk_unavailability_status CHECK (status IN ('REQUESTED','APPROVED','REJECTED')),
    CONSTRAINT chk_unavailability_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_unavailability_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE,
    CONSTRAINT fk_unavailability_requested_by FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_unavailability_decided_by FOREIGN KEY (decided_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_unavailability_resource_dates (resource_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 14. budget_transaction
-- ------------------------------------------------------------------------------
CREATE TABLE budget_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    txn_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(20) NOT NULL,
    resource_id BIGINT NULL,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_category CHECK (category IN ('LABOUR','HARDWARE','CLOUD','OTHER')),
    CONSTRAINT fk_transaction_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE SET NULL,
    INDEX idx_transaction_project_date (project_id, txn_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 15. conflict
-- ------------------------------------------------------------------------------
CREATE TABLE conflict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    project_id BIGINT NULL,
    milestone_id BIGINT NULL,
    resource_id BIGINT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    peak_value DECIMAL(10,2) NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    detected_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    dedup_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_conflict_severity CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT chk_conflict_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
    CONSTRAINT uq_conflict_dedup_key UNIQUE (dedup_key),
    CONSTRAINT fk_conflict_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    CONSTRAINT fk_conflict_milestone FOREIGN KEY (milestone_id) REFERENCES milestone(id) ON DELETE SET NULL,
    CONSTRAINT fk_conflict_resource FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE SET NULL,
    INDEX idx_conflict_status_severity (status, severity),
    INDEX idx_conflict_project (project_id),
    INDEX idx_conflict_resource (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 16. simulation_scenario
-- ------------------------------------------------------------------------------
CREATE TABLE simulation_scenario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_by BIGINT NOT NULL,
    baseline_json JSON NOT NULL,
    result_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scenario_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_scenario_creator FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_scenario_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 17. simulation_change
-- ------------------------------------------------------------------------------
CREATE TABLE simulation_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    allocation_id BIGINT NULL,
    from_resource_id BIGINT NULL,
    to_resource_id BIGINT NULL,
    milestone_id BIGINT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    new_pct INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_sim_change_type CHECK (change_type IN ('SWAP','ADD','REMOVE','CHANGE_PCT')),
    CONSTRAINT chk_sim_change_new_pct CHECK (new_pct IS NULL OR (new_pct BETWEEN 1 AND 100)),
    CONSTRAINT fk_sim_change_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenario(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_change_from_resource FOREIGN KEY (from_resource_id) REFERENCES resource(id) ON DELETE SET NULL,
    CONSTRAINT fk_sim_change_to_resource FOREIGN KEY (to_resource_id) REFERENCES resource(id) ON DELETE SET NULL,
    CONSTRAINT fk_sim_change_milestone FOREIGN KEY (milestone_id) REFERENCES milestone(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 18. notification
-- ------------------------------------------------------------------------------
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    conflict_id BIGINT NULL,
    severity VARCHAR(10) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_severity CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_conflict FOREIGN KEY (conflict_id) REFERENCES conflict(id) ON DELETE SET NULL,
    INDEX idx_notification_user_unread (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Seed System Settings (Section 8)
-- ------------------------------------------------------------------------------
INSERT INTO settings (setting_key, setting_value, description) VALUES
('burnout.threshold.pct', '85', 'Weekly average load percentage that counts toward burnout risk'),
('burnout.consecutive.weeks', '3', 'Consecutive weeks needed for BURNOUT_RISK alert'),
('burn.window.days', '14', 'Rolling window in days used for calculating spend burn rate'),
('budget.cpi.warning', '0.9', 'Cost Performance Index (CPI) threshold below which BUDGET_RISK WARNING is raised'),
('budget.critical.days', '14', 'Budget run-out within this many days of today raises CRITICAL risk'),
('deadline.warning.days', '7', 'Days before milestone end for triggering deadline alert'),
('deadline.progress.pct', '80', 'Progress percentage below which deadline alert is triggered'),
('delivery.warning.days', '5', 'Project delay up to this many working days is WARNING, above is CRITICAL'),
('skill.mismatch.pct', '50', 'Skill match score percentage below which assignment triggers SKILL_MISMATCH'),
('unstaffed.lookahead.days', '14', 'Milestones starting within this window must have developer allocated'),
('ranking.weight.skill', '0.5', 'Candidate ranking weight: Skill match score component'),
('ranking.weight.fit', '0.3', 'Candidate ranking weight: Capacity fit component'),
('ranking.weight.cost', '0.2', 'Candidate ranking weight: Cost cheapness component'),
('hours.per.day', '8', 'Working hours per developer day'),
('forecast.max.days', '1825', 'Safety cap on day-by-day delivery forecasting iterations');

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

