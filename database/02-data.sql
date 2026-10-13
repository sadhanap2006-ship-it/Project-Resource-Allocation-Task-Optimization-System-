-- =====================================================================
-- 02-data.sql - Project Resource Allocation System Seed Data
-- SIST CSE Capstone · Specification Section 14
-- Note: All dates relative to CURDATE() (T) so demo is always live.
-- Conflict and notification tables are populated dynamically by startup scan.
-- =====================================================================

USE resource_allocation;

-- ---------------------------------------------------------------------
-- 1. SKILLS (12 skills)
-- ---------------------------------------------------------------------
INSERT INTO skill (id, name, category) VALUES
(1, 'Java', 'Backend'),
(2, 'Spring Boot', 'Backend'),
(3, 'MySQL', 'Database'),
(4, 'Python', 'Data & AI'),
(5, 'Machine Learning', 'Data & AI'),
(6, 'Data Analysis', 'Data & AI'),
(7, 'React', 'Frontend'),
(8, 'JavaScript', 'Frontend'),
(9, 'UI/UX', 'Design'),
(10, 'AWS', 'Cloud'),
(11, 'Cloud', 'Cloud'),
(12, 'DevOps', 'DevOps');

-- ---------------------------------------------------------------------
-- 2. RESOURCES (5 Developers, 2 Hardware, 1 Cloud)
-- ---------------------------------------------------------------------
-- Developers
INSERT INTO resource (id, type, name, status, capacity_pct, cost_rate, cost_unit) VALUES
(1, 'DEVELOPER', 'Arun Kumar', 'ACTIVE', 100, 500.00, 'HOUR'),
(2, 'DEVELOPER', 'Priya Sharma', 'ACTIVE', 100, 550.00, 'HOUR'),
(3, 'DEVELOPER', 'Rahul Verma', 'ACTIVE', 100, 450.00, 'HOUR'),
(4, 'DEVELOPER', 'Divya Nair', 'ACTIVE', 100, 600.00, 'HOUR'),
(5, 'DEVELOPER', 'Karthik Raj', 'ACTIVE', 100, 650.00, 'HOUR');

-- Developer Profiles
INSERT INTO developer_profile (resource_id, email, job_title) VALUES
(1, 'arun@prs.local', 'Backend Developer'),
(2, 'priya@prs.local', 'ML Engineer'),
(3, 'rahul@prs.local', 'Frontend Developer'),
(4, 'divya@prs.local', 'DevOps & Cloud Specialist'),
(5, 'karthik@prs.local', 'Senior Full Stack Engineer');

-- Hardware Resources
INSERT INTO resource (id, type, name, status, capacity_pct, cost_rate, cost_unit) VALUES
(6, 'HARDWARE', 'GPU Workstation', 'ACTIVE', 100, 2000.00, 'DAY'),
(7, 'HARDWARE', 'IoT Test Kit', 'ACTIVE', 100, 800.00, 'DAY');

-- Hardware Assets
INSERT INTO hardware_asset (resource_id, asset_tag, lab_location) VALUES
(6, 'GPU-01', 'AI Lab Room 302'),
(7, 'IOT-01', 'Embedded Lab Room 105');

-- Cloud Resources
INSERT INTO resource (id, type, name, status, capacity_pct, cost_rate, cost_unit) VALUES
(8, 'CLOUD', 'AWS EC2 Pool', 'ACTIVE', 100, 8.00, 'UNIT');

-- Cloud Resource Details
INSERT INTO cloud_resource (resource_id, provider, unit_name, daily_quota_units, monthly_cap) VALUES
(8, 'AWS', 'vCPU-hour', 200.00, 40000.00);

-- ---------------------------------------------------------------------
-- 3. USERS (Admin, PM Meena, 5 Developers linked to resources)
-- Real BCrypt hash for 'password123':
-- $2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K
-- ---------------------------------------------------------------------
INSERT INTO users (id, name, email, password_hash, role, resource_id, active) VALUES
(1, 'System Administrator', 'admin@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'ADMIN', NULL, TRUE),
(2, 'Meena Iyer', 'pm@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'PM', NULL, TRUE),
(3, 'Arun Kumar', 'arun@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'DEVELOPER', 1, TRUE),
(4, 'Priya Sharma', 'priya@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'DEVELOPER', 2, TRUE),
(5, 'Rahul Verma', 'rahul@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'DEVELOPER', 3, TRUE),
(6, 'Divya Nair', 'divya@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'DEVELOPER', 4, TRUE),
(7, 'Karthik Raj', 'karthik@prs.local', '$2a$10$bCKGsXNQwK3qtrb3bi7C6.UYi875M.00Zrrq70p5OjcmY2wuHNG2K', 'DEVELOPER', 5, TRUE);

-- ---------------------------------------------------------------------
-- 4. DEVELOPER SKILLS
-- ---------------------------------------------------------------------
-- Arun: Java 3, Spring Boot 2, MySQL 2
INSERT INTO resource_skill (resource_id, skill_id, proficiency) VALUES
(1, 1, 3), (1, 2, 2), (1, 3, 2);

-- Priya: Python 3, Machine Learning 3, Data Analysis 3
INSERT INTO resource_skill (resource_id, skill_id, proficiency) VALUES
(2, 4, 3), (2, 5, 3), (2, 6, 3);

-- Rahul: React 3, JavaScript 3, UI/UX 2
INSERT INTO resource_skill (resource_id, skill_id, proficiency) VALUES
(3, 7, 3), (3, 8, 3), (3, 9, 2);

-- Divya: AWS 2, Cloud 3, DevOps 3
INSERT INTO resource_skill (resource_id, skill_id, proficiency) VALUES
(4, 10, 2), (4, 11, 3), (4, 12, 3);

-- Karthik: Java 3, Spring Boot 3, AWS 2
INSERT INTO resource_skill (resource_id, skill_id, proficiency) VALUES
(5, 1, 3), (5, 2, 3), (5, 10, 2);

-- ---------------------------------------------------------------------
-- 5. PROJECTS (PM = Meena Iyer, user_id = 2)
-- ---------------------------------------------------------------------
INSERT INTO project (id, code, name, description, pm_user_id, start_date, end_date, status, total_budget) VALUES
(1, 'ALPHA', 'Hospital Management System', 'End-to-end patient management and booking architecture', 2, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'ACTIVE', 500000.00),
(2, 'BETA', 'AI Student Analytics', 'Predictive analytics and assessment pipeline for universities', 2, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 75 DAY), 'ACTIVE', 400000.00),
(3, 'GAMMA', 'Cloud Monitoring Platform', 'Distributed telemetry and real-time observability framework', 2, DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'ACTIVE', 600000.00);

-- ---------------------------------------------------------------------
-- 6. MILESTONES
-- ---------------------------------------------------------------------
-- ALPHA Milestones
INSERT INTO milestone (id, project_id, name, description, start_date, end_date, status, progress_pct, estimated_effort_hours) VALUES
(1, 1, 'ALPHA M1 Backend API Development', 'REST services and database persistence', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'IN_PROGRESS', 30, 320.00),
(2, 1, 'ALPHA M2 Patient Portal UI', 'Responsive UI portal for patient records', DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'IN_PROGRESS', 20, 200.00);

-- BETA Milestones
INSERT INTO milestone (id, project_id, name, description, start_date, end_date, status, progress_pct, estimated_effort_hours) VALUES
(3, 2, 'BETA M1 Data Pipeline API', 'Data ingestion and cleaning pipeline', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'IN_PROGRESS', 15, 180.00),
(4, 2, 'BETA M2 ML Model Training', 'Training and evaluating predictive models', DATE_SUB(CURDATE(), INTERVAL 28 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'IN_PROGRESS', 40, 250.00);

-- GAMMA Milestones
INSERT INTO milestone (id, project_id, name, description, start_date, end_date, status, progress_pct, estimated_effort_hours) VALUES
(5, 3, 'GAMMA M1 Monitoring Agent', 'Lightweight system metric daemon', DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 'IN_PROGRESS', 25, 220.00),
(6, 3, 'GAMMA M2 Cloud Infrastructure Automation', 'Terraform and infrastructure provisioning', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY), 'PLANNED', 0, 160.00),
(7, 3, 'GAMMA M3 Monitoring Dashboard', 'Real-time telemetry and visualization', DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'PLANNED', 0, 140.00);

-- ---------------------------------------------------------------------
-- 7. MILESTONE SKILL REQUIREMENTS
-- ---------------------------------------------------------------------
-- ALPHA M1: Java 2, Spring Boot 2, MySQL 2
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(1, 1, 2, TRUE),
(1, 2, 2, TRUE),
(1, 3, 2, TRUE);

-- ALPHA M2: React 2, JavaScript 2
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(2, 7, 2, TRUE),
(2, 8, 2, TRUE);

-- BETA M1: Java 2, Spring Boot 2
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(3, 1, 2, TRUE),
(3, 2, 2, TRUE);

-- BETA M2: Python 2, Machine Learning 2
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(4, 4, 2, TRUE),
(4, 5, 2, TRUE);

-- GAMMA M1: Cloud 2, DevOps 2
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(5, 11, 2, TRUE),
(5, 12, 2, TRUE);

-- GAMMA M2: AWS 3 mandatory (Nobody holds level 3 -> CRITICAL SKILL_GAP)
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(6, 10, 3, TRUE);

-- GAMMA M3: JavaScript 2, UI/UX 2 (Starts T+7, unstaffed -> WARNING UNSTAFFED_MILESTONE)
INSERT INTO milestone_skill_requirement (milestone_id, skill_id, min_proficiency, mandatory) VALUES
(7, 8, 2, TRUE),
(7, 9, 2, TRUE);

-- ---------------------------------------------------------------------
-- 8. ALLOCATIONS (The 11 Demo Situations per Section 14)
-- ---------------------------------------------------------------------
-- #1: Arun -> ALPHA M1, 60%, T-10 to T+10
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 60, FALSE);

-- #2: Arun -> BETA M1, 50%, T to T+15 (with #1: OVER_ALLOCATION 110% on T to T+10 weekdays)
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(2, 1, 3, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 50, TRUE);

-- #3: Rahul -> ALPHA M2, 80%, T-5 to T+25
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(3, 3, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 80, FALSE);

-- #4: Rahul -> ALPHA M1, 10%, T to T+7 (SKILL_MISMATCH match 0%)
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(4, 3, 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 10, TRUE);

-- #5: Priya -> BETA M2, 90%, T-21 to T+14 (BURNOUT_RISK)
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(5, 2, 4, DATE_SUB(CURDATE(), INTERVAL 21 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 90, FALSE);

-- #6: Karthik -> ALPHA M1, 40%, T to T+15
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(6, 5, 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 40, FALSE);

-- #7: Divya -> GAMMA M1, 50%, T-7 to T+40
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(7, 4, 5, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 50, FALSE);

-- #8: GPU Workstation -> BETA M2, 60%, T to T+10
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(8, 6, 4, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 60, FALSE);

-- #9: GPU Workstation -> GAMMA M1, 60%, T+5 to T+12 (with #8: HW OVER_ALLOCATION 120%)
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(9, 6, 5, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), 60, TRUE);

-- #10: AWS EC2 Pool -> GAMMA M1, 70%, T to T+10
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(10, 8, 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 70, FALSE);

-- #11: AWS EC2 Pool -> BETA M2, 50%, T+5 to T+10 (with #10: CLOUD_QUOTA 120%)
INSERT INTO allocation (id, resource_id, milestone_id, start_date, end_date, allocation_pct, override_confirmed) VALUES
(11, 8, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 50, TRUE);

-- ---------------------------------------------------------------------
-- 9. UNAVAILABILITY
-- Rahul approved LEAVE T+10 to T+14 (with allocation #3 -> LEAVE_CONFLICT CRITICAL)
-- ---------------------------------------------------------------------
INSERT INTO unavailability (id, resource_id, start_date, end_date, type, status, reason, requested_by, decided_by) VALUES
(1, 3, DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'LEAVE', 'APPROVED', 'Annual personal leave', 5, 2);

-- ---------------------------------------------------------------------
-- 10. BUDGET TRANSACTIONS
-- ALPHA: Total ₹300,000 spend (₹160,000 T-60 to T-14 + ₹140,000 in last 14 days)
-- Result: burn rate ₹10,000/day, run-out around T+20 < end T+60 (BUDGET_RISK EARLY_RUNOUT)
-- ---------------------------------------------------------------------
-- ALPHA Historical spend (8 transactions * ₹20,000 = ₹160,000)
INSERT INTO budget_transaction (project_id, txn_date, amount, category, resource_id, note) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 56 DAY), 20000.00, 'LABOUR', 1, 'Sprint 1 labour cost'),
(1, DATE_SUB(CURDATE(), INTERVAL 50 DAY), 20000.00, 'LABOUR', 1, 'Sprint 1 architecture design'),
(1, DATE_SUB(CURDATE(), INTERVAL 44 DAY), 20000.00, 'HARDWARE', 6, 'GPU setup cost'),
(1, DATE_SUB(CURDATE(), INTERVAL 38 DAY), 20000.00, 'LABOUR', 3, 'UI prototyping'),
(1, DATE_SUB(CURDATE(), INTERVAL 32 DAY), 20000.00, 'CLOUD', 8, 'Cloud infra staging'),
(1, DATE_SUB(CURDATE(), INTERVAL 26 DAY), 20000.00, 'LABOUR', 5, 'Integration testing'),
(1, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 20000.00, 'LABOUR', 1, 'Database optimization'),
(1, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 20000.00, 'OTHER', NULL, 'Security compliance audit');

-- ALPHA Last 14 days spend (14 days * ₹10,000 = ₹140,000)
INSERT INTO budget_transaction (project_id, txn_date, amount, category, resource_id, note) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 13 DAY), 10000.00, 'LABOUR', 1, 'Daily burn burn-rate window day 1'),
(1, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 10000.00, 'LABOUR', 3, 'Daily burn burn-rate window day 2'),
(1, DATE_SUB(CURDATE(), INTERVAL 11 DAY), 10000.00, 'LABOUR', 5, 'Daily burn burn-rate window day 3'),
(1, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 10000.00, 'HARDWARE', 6, 'Daily burn burn-rate window day 4'),
(1, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 10000.00, 'LABOUR', 1, 'Daily burn burn-rate window day 5'),
(1, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 10000.00, 'LABOUR', 3, 'Daily burn burn-rate window day 6'),
(1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 10000.00, 'CLOUD', 8, 'Daily burn burn-rate window day 7'),
(1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 10000.00, 'LABOUR', 5, 'Daily burn burn-rate window day 8'),
(1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 10000.00, 'LABOUR', 1, 'Daily burn burn-rate window day 9'),
(1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 10000.00, 'LABOUR', 3, 'Daily burn burn-rate window day 10'),
(1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 10000.00, 'HARDWARE', 6, 'Daily burn burn-rate window day 11'),
(1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 10000.00, 'LABOUR', 1, 'Daily burn burn-rate window day 12'),
(1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 10000.00, 'LABOUR', 5, 'Daily burn burn-rate window day 13'),
(1, CURDATE(), 10000.00, 'LABOUR', 1, 'Daily burn burn-rate window day 14');

-- BETA and GAMMA modest spend
INSERT INTO budget_transaction (project_id, txn_date, amount, category, resource_id, note) VALUES
(2, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 15000.00, 'LABOUR', 2, 'Model exploration'),
(2, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 25000.00, 'HARDWARE', 6, 'GPU compute hours'),
(2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 10000.00, 'LABOUR', 1, 'API pipeline integration'),
(3, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 12000.00, 'LABOUR', 4, 'Telemetry agent baseline'),
(3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 18000.00, 'CLOUD', 8, 'Observability cluster allocation');
