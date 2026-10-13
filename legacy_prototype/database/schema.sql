-- ============================================================================
-- SIST CSE AI Capstone Project — Project Resource Allocation System
-- Relational Database Schema (MySQL 8.0, 3NF Normalized)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS resource_allocation;
USE resource_allocation;

-- Disable foreign key checks for clean teardown and reconstruction
-- Disable foreign key checks for clean teardown and reconstruction
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS task_feedback;
DROP TABLE IF EXISTS profile_audit_logs;
DROP TABLE IF EXISTS skill_endorsements;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS certifications;
DROP TABLE IF EXISTS what_if_scenarios;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS budget_transactions;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS developer_availability;
DROP TABLE IF EXISTS resource_allocations;
DROP TABLE IF EXISTS milestone_skills;
DROP TABLE IF EXISTS milestones;
DROP TABLE IF EXISTS project_skills;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS developer_skills;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS developers;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Users Table (Authentication and Role-Based Access)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE', -- 'EMPLOYEE', 'MANAGER', 'ADMIN'
    profile_completed BOOLEAN NOT NULL DEFAULT TRUE,
    reset_token VARCHAR(255) NULL,
    reset_token_expiry TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Developers Table (Resource Profiles)
CREATE TABLE developers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    role_title VARCHAR(100) NOT NULL,
    department VARCHAR(100) DEFAULT 'Engineering',
    hourly_cost DECIMAL(10, 2) NOT NULL CHECK (hourly_cost >= 0),
    availability_percentage INT NOT NULL DEFAULT 100 CHECK (availability_percentage BETWEEN 0 AND 100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'ON_LEAVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Skills Table
CREATE TABLE skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    canonical_name VARCHAR(80) NOT NULL,
    synonyms TEXT NULL,
    category VARCHAR(50) DEFAULT 'General',
    is_approved BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Developer Skills Junction Table (Many-to-Many 3NF with Verification & Proficiency)
CREATE TABLE developer_skills (
    developer_id INT NOT NULL,
    skill_id INT NOT NULL,
    proficiency VARCHAR(30) NOT NULL DEFAULT 'INTERMEDIATE', -- 'BEGINNER', 'INTERMEDIATE', 'EXPERT'
    years_experience INT NOT NULL DEFAULT 1,
    status VARCHAR(40) NOT NULL DEFAULT 'VERIFIED_MANAGER', -- 'PENDING', 'VERIFIED_MANAGER', 'VERIFIED_TRACK_RECORD', 'REJECTED'
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL', -- 'MANUAL', 'EXTRACTED_TEXT', 'EXTRACTED_CERT', 'TASK_HISTORY'
    interested_in_future BOOLEAN NOT NULL DEFAULT FALSE,
    endorsements_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (developer_id, skill_id),
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Projects Table
CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_budget DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_budget >= 0),
    current_spend DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (current_spend >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED', -- 'PLANNED', 'ACTIVE', 'AT_RISK', 'COMPLETED', 'ON_HOLD'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_proj_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Project Required Skills Junction Table
CREATE TABLE project_skills (
    project_id INT NOT NULL,
    skill_id INT NOT NULL,
    PRIMARY KEY (project_id, skill_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Milestones Table
CREATE TABLE milestones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    estimated_effort_hours DECIMAL(8, 2) NOT NULL DEFAULT 40.0 CHECK (estimated_effort_hours > 0),
    progress_percentage INT NOT NULL DEFAULT 0 CHECK (progress_percentage BETWEEN 0 AND 100),
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED', -- 'NOT_STARTED', 'IN_PROGRESS', 'AT_RISK', 'COMPLETED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_milestone_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Milestone Required Skills Junction Table
CREATE TABLE milestone_skills (
    milestone_id INT NOT NULL,
    skill_id INT NOT NULL,
    PRIMARY KEY (milestone_id, skill_id),
    FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Resource Allocations Table
CREATE TABLE resource_allocations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    project_id INT NOT NULL,
    milestone_id INT NOT NULL,
    allocation_percentage INT NOT NULL CHECK (allocation_percentage BETWEEN 1 AND 100),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    hourly_cost_snapshot DECIMAL(10, 2) NOT NULL,
    estimated_hours DECIMAL(8, 2) NOT NULL DEFAULT 40.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    CONSTRAINT chk_alloc_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Developer Availability & Planned Leave Table
CREATE TABLE developer_availability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255) DEFAULT 'Planned Vacation',
    status VARCHAR(30) NOT NULL DEFAULT 'APPROVED', -- 'PENDING', 'APPROVED', 'REJECTED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. Budgets and Burn Rate Table
CREATE TABLE budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL UNIQUE,
    total_budget DECIMAL(12, 2) NOT NULL CHECK (total_budget >= 0),
    current_spend DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (current_spend >= 0),
    allocated_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'INR',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11b. Budget Transactions Table (Audit Trail & Rolling-Window Burn-Rate Tracking)
CREATE TABLE budget_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    transaction_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. In-App Notifications Table
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM_ALERT', -- 'OVER_ALLOCATION', 'LEAVE_CONFLICT', 'SKILL_GAP', 'BUDGET_WARNING'
    severity VARCHAR(30) NOT NULL DEFAULT 'INFO', -- 'CRITICAL', 'WARNING', 'INFO'
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. What-If Scenarios Table (Saved Simulation Comparison Records)
CREATE TABLE what_if_scenarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    milestone_id INT NOT NULL,
    current_developer_id INT NOT NULL,
    proposed_developer_id INT NOT NULL,
    allocation_percentage INT NOT NULL,
    cost_difference DECIMAL(10, 2) NOT NULL,
    delivery_days_delta INT NOT NULL,
    scenario_verdict VARCHAR(50) NOT NULL,
    recommendation TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    FOREIGN KEY (current_developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (proposed_developer_id) REFERENCES developers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. Certifications Table (Uploaded & Verified Credentials)
CREATE TABLE certifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    name VARCHAR(200) NOT NULL,
    issuing_body VARCHAR(150) NOT NULL,
    issue_date DATE NOT NULL,
    credential_id VARCHAR(100),
    file_name VARCHAR(255),
    file_data LONGTEXT,
    confidence_score DECIMAL(5, 2) DEFAULT 0.95,
    verified BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. Leave Requests Table (Employee Submissions & Manager Approvals)
CREATE TABLE leave_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    reviewed_by INT NULL,
    reviewer_comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_req_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. Skill Endorsements Table (Peer & Manager Recognition)
CREATE TABLE skill_endorsements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    skill_id INT NOT NULL,
    endorsed_by_user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    FOREIGN KEY (endorsed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. Profile Audit Logs Table (Full Traceability for Skill Changes)
CREATE TABLE profile_audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    developer_id INT NOT NULL,
    actor_user_id INT NOT NULL,
    action_type VARCHAR(60) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 18. Task Feedback Table (Post-Task Review & Match Score Tuning)
CREATE TABLE task_feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    milestone_id INT NOT NULL,
    developer_id INT NOT NULL,
    manager_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    FOREIGN KEY (developer_id) REFERENCES developers(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Helpful performance indexes
CREATE INDEX idx_alloc_dev ON resource_allocations(developer_id);
CREATE INDEX idx_alloc_dates ON resource_allocations(start_date, end_date);
CREATE INDEX idx_leave_dev_dates ON developer_availability(developer_id, start_date, end_date);
CREATE INDEX idx_milestone_proj ON milestones(project_id);
CREATE INDEX idx_cert_dev ON certifications(developer_id);
CREATE INDEX idx_leave_req_dev ON leave_requests(developer_id);
