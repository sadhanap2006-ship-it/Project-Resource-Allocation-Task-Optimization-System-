-- =====================================================================
--  PROJECT RESOURCE ALLOCATION SYSTEM  -  DATABASE SCHEMA
--  SIST CSE AI Capstone Project
--  Target: MySQL 8.0
--
--  How to run:
--     mysql -u root -p < database/schema.sql
--     mysql -u root -p < database/sample-data.sql
-- =====================================================================

DROP DATABASE IF EXISTS resource_allocation_db;
CREATE DATABASE resource_allocation_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE resource_allocation_db;

-- ---------------------------------------------------------------------
-- 1. developers  --  the "resource profile" of every team member
-- ---------------------------------------------------------------------
CREATE TABLE developers (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(100)  NOT NULL,
    email                   VARCHAR(150)  NOT NULL,
    role                    VARCHAR(80)   NOT NULL,
    hourly_cost             DECIMAL(10,2) NOT NULL,
    availability_percentage INT           NOT NULL DEFAULT 100,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_developers_email (email),
    CONSTRAINT chk_dev_cost         CHECK (hourly_cost >= 0),
    CONSTRAINT chk_dev_availability CHECK (availability_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_dev_status       CHECK (status IN ('ACTIVE','INACTIVE','ON_LEAVE'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. users  --  login accounts (used for the role-based dashboards)
--    NOTE: this is a college demo login. A production system would store
--    a hashed password (BCrypt) instead of the plain text kept here.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    developer_id BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT fk_users_developer FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE SET NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('PROJECT_MANAGER','DEVELOPER'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. skills  --  master list of skills
-- ---------------------------------------------------------------------
CREATE TABLE skills (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    name     VARCHAR(80) NOT NULL,
    category VARCHAR(80) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_skills_name (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. developer_skills  --  which developer knows which skill
-- ---------------------------------------------------------------------
CREATE TABLE developer_skills (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    developer_id      BIGINT      NOT NULL,
    skill_id          BIGINT      NOT NULL,
    proficiency_level VARCHAR(20) NOT NULL DEFAULT 'INTERMEDIATE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dev_skill (developer_id, skill_id),
    CONSTRAINT fk_ds_developer FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT fk_ds_skill     FOREIGN KEY (skill_id)     REFERENCES skills(id)     ON DELETE CASCADE,
    CONSTRAINT chk_ds_level CHECK (proficiency_level IN ('BEGINNER','INTERMEDIATE','EXPERT'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. projects
-- ---------------------------------------------------------------------
CREATE TABLE projects (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(1000) NULL,
    start_date  DATE          NOT NULL,
    end_date    DATE          NOT NULL,
    budget      DECIMAL(14,2) NOT NULL DEFAULT 0,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PLANNED',
    PRIMARY KEY (id),
    CONSTRAINT chk_proj_dates  CHECK (end_date >= start_date),
    CONSTRAINT chk_proj_budget CHECK (budget >= 0),
    CONSTRAINT chk_proj_status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','AT_RISK'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. milestones
--    estimated_effort_days = planned effort in DEVELOPER-DAYS.
--    It is the input to the delivery-date formula:
--        duration = ceil(estimated_effort_days / total capacity)
-- ---------------------------------------------------------------------
CREATE TABLE milestones (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    project_id            BIGINT        NOT NULL,
    name                  VARCHAR(150)  NOT NULL,
    description           VARCHAR(1000) NULL,
    start_date            DATE          NOT NULL,
    end_date              DATE          NOT NULL,
    progress_percentage   INT           NOT NULL DEFAULT 0,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PLANNED',
    estimated_effort_days INT           NOT NULL DEFAULT 10,
    PRIMARY KEY (id),
    KEY idx_ms_project (project_id),
    CONSTRAINT fk_ms_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_ms_dates    CHECK (end_date >= start_date),
    CONSTRAINT chk_ms_progress CHECK (progress_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_ms_effort   CHECK (estimated_effort_days > 0),
    CONSTRAINT chk_ms_status   CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','DELAYED','AT_RISK'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. milestone_skills  --  skills a milestone requires
-- ---------------------------------------------------------------------
CREATE TABLE milestone_skills (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    milestone_id BIGINT NOT NULL,
    skill_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ms_skill (milestone_id, skill_id),
    CONSTRAINT fk_mss_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    CONSTRAINT fk_mss_skill     FOREIGN KEY (skill_id)     REFERENCES skills(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. resource_allocations
-- ---------------------------------------------------------------------
CREATE TABLE resource_allocations (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    developer_id          BIGINT NOT NULL,
    project_id            BIGINT NOT NULL,
    milestone_id          BIGINT NULL,
    start_date            DATE   NOT NULL,
    end_date              DATE   NOT NULL,
    allocation_percentage INT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_alloc_developer (developer_id),
    KEY idx_alloc_project   (project_id),
    KEY idx_alloc_milestone (milestone_id),
    CONSTRAINT fk_alloc_developer FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_project   FOREIGN KEY (project_id)   REFERENCES projects(id)   ON DELETE CASCADE,
    CONSTRAINT fk_alloc_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    CONSTRAINT chk_alloc_dates   CHECK (end_date >= start_date),
    CONSTRAINT chk_alloc_percent CHECK (allocation_percentage BETWEEN 1 AND 100)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. developer_leave  --  planned leave / unavailability
-- ---------------------------------------------------------------------
CREATE TABLE developer_leave (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    developer_id BIGINT       NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    reason       VARCHAR(255) NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    PRIMARY KEY (id),
    KEY idx_leave_developer (developer_id),
    CONSTRAINT fk_leave_developer FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT chk_leave_dates  CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_status CHECK (status IN ('PLANNED','APPROVED','CANCELLED'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. project_budgets  --  one budget row per project
-- ---------------------------------------------------------------------
CREATE TABLE project_budgets (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    project_id       BIGINT        NOT NULL,
    total_budget     DECIMAL(14,2) NOT NULL DEFAULT 0,
    current_spending DECIMAL(14,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_project (project_id),
    CONSTRAINT fk_budget_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_budget_total    CHECK (total_budget >= 0),
    CONSTRAINT chk_budget_spending CHECK (current_spending >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. budget_transactions  --  every amount actually spent
-- ---------------------------------------------------------------------
CREATE TABLE budget_transactions (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    project_budget_id BIGINT        NOT NULL,
    transaction_date  DATE          NOT NULL,
    amount            DECIMAL(14,2) NOT NULL,
    category          VARCHAR(80)   NULL,
    description       VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    KEY idx_txn_budget (project_budget_id),
    CONSTRAINT fk_txn_budget FOREIGN KEY (project_budget_id) REFERENCES project_budgets(id) ON DELETE CASCADE,
    CONSTRAINT chk_txn_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. notifications  --  in-app alerts
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    type                VARCHAR(30)  NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    message             VARCHAR(500) NOT NULL,
    related_entity_type VARCHAR(50)  NULL,
    related_entity_id   BIGINT       NULL,
    developer_id        BIGINT       NULL,
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notif_developer (developer_id),
    CONSTRAINT fk_notif_developer FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT chk_notif_type CHECK (type IN
        ('OVER_ALLOCATION','SKILL_GAP','LEAVE_CONFLICT','BUDGET_RISK','MILESTONE_DELAY','MILESTONE_DEADLINE')),
    CONSTRAINT chk_notif_severity CHECK (severity IN ('INFO','WARNING','CRITICAL'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 13. simulation_scenarios  --  saved what-if scenarios
--     Rows appear here ONLY when the manager presses "Save scenario".
--     Running a simulation never writes to this table.
-- ---------------------------------------------------------------------
CREATE TABLE simulation_scenarios (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    scenario_name           VARCHAR(150)  NOT NULL,
    project_id              BIGINT        NOT NULL,
    milestone_id            BIGINT        NULL,
    current_developer_id    BIGINT        NOT NULL,
    new_developer_id        BIGINT        NOT NULL,
    allocation_percentage   INT           NOT NULL,
    current_cost            DECIMAL(14,2) NOT NULL DEFAULT 0,
    simulated_cost          DECIMAL(14,2) NOT NULL DEFAULT 0,
    current_skill_match     INT           NOT NULL DEFAULT 0,
    simulated_skill_match   INT           NOT NULL DEFAULT 0,
    current_conflicts       INT           NOT NULL DEFAULT 0,
    simulated_conflicts     INT           NOT NULL DEFAULT 0,
    current_delivery_date   DATE          NULL,
    simulated_delivery_date DATE          NULL,
    budget_impact           DECIMAL(14,2) NOT NULL DEFAULT 0,
    created_at              DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sim_project (project_id),
    CONSTRAINT fk_sim_project   FOREIGN KEY (project_id)           REFERENCES projects(id)   ON DELETE CASCADE,
    CONSTRAINT fk_sim_milestone FOREIGN KEY (milestone_id)         REFERENCES milestones(id) ON DELETE SET NULL,
    CONSTRAINT fk_sim_curdev    FOREIGN KEY (current_developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_newdev    FOREIGN KEY (new_developer_id)     REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT chk_sim_percent  CHECK (allocation_percentage BETWEEN 1 AND 100)
) ENGINE=InnoDB;
