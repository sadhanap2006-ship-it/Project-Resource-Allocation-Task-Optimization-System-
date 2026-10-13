-- ============================================================================
-- SIST CSE AI Capstone Project — Project Resource Allocation System
-- Realistic Sample Demonstration Seed Data
-- ============================================================================

USE resource_allocation;

-- 1. Insert Users (Project Manager, Developers/Employees, Admin)
INSERT INTO users (id, name, email, password, role, profile_completed) VALUES
(1, 'Rajesh Ramanathan', 'manager@company.com', 'admin123', 'MANAGER', TRUE),
(2, 'Arun Kumar', 'arun@company.com', 'dev123', 'EMPLOYEE', TRUE),
(3, 'Priya Sharma', 'priya@company.com', 'dev123', 'EMPLOYEE', TRUE),
(4, 'Admin Executive', 'admin@company.com', 'admin123', 'ADMIN', TRUE);

-- 2. Insert Skills (10 Core & Extended Skills with canonical names and synonyms)
INSERT INTO skills (id, name, canonical_name, synonyms, category, is_approved) VALUES
(1, 'Java', 'Java', 'java,core java,java 17,java 21', 'Backend', TRUE),
(2, 'MySQL', 'MySQL', 'mysql,mysql 8,mariadb,rdbms', 'Database', TRUE),
(3, 'REST API', 'REST API', 'rest,restful api,api design,web services', 'Backend', TRUE),
(4, 'React', 'React', 'react,react.js,reactjs,react native', 'Frontend', TRUE),
(5, 'Docker', 'Docker', 'docker,containerization,docker compose', 'DevOps', TRUE),
(6, 'AWS', 'AWS', 'aws,amazon web services,ec2,s3,lambda,cloud architecture', 'Cloud', TRUE),
(7, 'Python', 'Python', 'python,python3,py,django,fastapi', 'AI/Backend', TRUE),
(8, 'Security', 'Security', 'security,infosec,oauth2,jwt,cybersecurity', 'Infrastructure', TRUE),
(9, 'Kafka', 'Kafka', 'kafka,apache kafka,event streaming', 'Backend', TRUE),
(10, 'Kubernetes', 'Kubernetes', 'k8s,kubernetes,container orchestration', 'DevOps', TRUE);

-- 3. Insert Developers (5 Developers)
INSERT INTO developers (id, user_id, name, email, role_title, department, hourly_cost, availability_percentage, status) VALUES
(1, 2, 'Arun Kumar', 'arun@company.com', 'Backend Java Developer', 'Engineering', 600.00, 100, 'ACTIVE'),
(2, 3, 'Priya Sharma', 'priya@company.com', 'Full-Stack Lead Engineer', 'Engineering', 800.00, 100, 'ACTIVE'),
(3, NULL, 'Rahul Verma', 'rahul@company.com', 'DevOps & Cloud Engineer', 'Infrastructure', 750.00, 100, 'ACTIVE'),
(4, NULL, 'Sneha Patel', 'sneha@company.com', 'Frontend UI/UX Specialist', 'Engineering', 500.00, 100, 'ACTIVE'),
(5, NULL, 'Vikram Singh', 'vikram@company.com', 'AI & Data Systems Engineer', 'Data Science', 700.00, 100, 'ACTIVE');

-- 4. Map Developer Skills (Junction Table with verification status & proficiency)
INSERT INTO developer_skills (developer_id, skill_id, proficiency, years_experience, status, source, interested_in_future, endorsements_count) VALUES
(1, 1, 'EXPERT', 5, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', FALSE, 4),     -- Arun: Java (Verified by track record)
(1, 2, 'INTERMEDIATE', 3, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 2),            -- Arun: MySQL (Verified by manager)
(1, 3, 'INTERMEDIATE', 3, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', TRUE, 3),   -- Arun: REST API (Verified by track record)
(1, 6, 'BEGINNER', 1, 'PENDING', 'EXTRACTED_CERT', TRUE, 0),                  -- Arun: AWS (Pending verification from cert)
(2, 1, 'EXPERT', 6, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 5),                  -- Priya: Java
(2, 2, 'EXPERT', 5, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 3),                  -- Priya: MySQL
(2, 3, 'EXPERT', 5, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', FALSE, 4),        -- Priya: REST API
(2, 4, 'EXPERT', 4, 'VERIFIED_MANAGER', 'MANUAL', TRUE, 6),                   -- Priya: React
(3, 5, 'EXPERT', 4, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 3),                  -- Rahul: Docker
(3, 6, 'EXPERT', 4, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', FALSE, 5),        -- Rahul: AWS
(3, 8, 'INTERMEDIATE', 3, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 2),            -- Rahul: Security
(4, 3, 'INTERMEDIATE', 2, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 1),            -- Sneha: REST API
(4, 4, 'EXPERT', 4, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', FALSE, 4),        -- Sneha: React
(5, 1, 'INTERMEDIATE', 3, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 2),            -- Vikram: Java
(5, 2, 'INTERMEDIATE', 3, 'VERIFIED_MANAGER', 'MANUAL', FALSE, 1),            -- Vikram: MySQL
(5, 7, 'EXPERT', 4, 'VERIFIED_TRACK_RECORD', 'TASK_HISTORY', TRUE, 5);         -- Vikram: Python

-- 5. Insert Projects (4 Diverse Enterprise Projects)
INSERT INTO projects (id, name, description, start_date, end_date, total_budget, current_spend, status) VALUES
(1, 'Project Alpha — Cloud Microservices Platform', 'Scalable distributed backend migration to high-availability microservices architecture.', '2026-10-01', '2026-12-15', 500000.00, 350000.00, 'ACTIVE'),
(2, 'Project Beta — Enterprise AI Analytics Portal', 'Real-time telemetry and predictive forecasting dashboard for operations.', '2026-10-05', '2026-12-30', 800000.00, 280000.00, 'ACTIVE'),
(3, 'Project Gamma — Healthcare Patient Portal API', 'HIPAA-compliant patient appointment booking and clinical records API.', '2026-10-15', '2027-01-20', 450000.00, 120000.00, 'PLANNED'),
(4, 'Project Delta — IoT Edge Telemetry Pipeline', 'High-throughput edge sensor aggregation pipeline with streaming analytics.', '2026-11-01', '2027-02-28', 600000.00, 50000.00, 'PLANNED');

-- 6. Insert Milestones
INSERT INTO milestones (id, project_id, name, description, start_date, end_date, estimated_effort_hours, progress_percentage, status) VALUES
-- Project Alpha Milestones
(1, 1, 'Core Auth & Security Service', 'OAuth2/Session tokens and role-based access module.', '2026-10-01', '2026-10-20', 80.0, 75, 'IN_PROGRESS'),
(2, 1, 'Order Processing Pipeline', 'High-throughput transactional queue and database ingestion.', '2026-10-10', '2026-11-05', 120.0, 30, 'IN_PROGRESS'),
(3, 1, 'Cloud Deployment & Monitoring', 'Containerization and production cluster deployment.', '2026-11-01', '2026-12-10', 90.0, 0, 'NOT_STARTED'),

-- Project Beta Milestones
(4, 2, 'AI Model Serving REST API', 'REST wrappers around predictive data science pipelines.', '2026-10-08', '2026-10-28', 100.0, 40, 'IN_PROGRESS'),
(5, 2, 'Cloud Infrastructure Setup', 'Kubernetes cluster deployment and VPC networking.', '2026-10-10', '2026-10-25', 80.0, 20, 'IN_PROGRESS'),
(6, 2, 'Interactive Analytics UI', 'Dynamic Chart.js executive KPI dashboards.', '2026-11-01', '2026-12-20', 110.0, 0, 'NOT_STARTED'),

-- Project Gamma Milestones
(7, 3, 'HIPAA Security & Audit Logging', 'Audit trails and clinical data access controls.', '2026-10-15', '2026-11-15', 70.0, 0, 'NOT_STARTED'),
(8, 3, 'Patient Mobile API Integration', 'Mobile client endpoints and push notification dispatch.', '2026-11-10', '2027-01-10', 95.0, 0, 'NOT_STARTED');

-- 7. Map Milestone Required Skills
INSERT INTO milestone_skills (milestone_id, skill_id) VALUES
(1, 1), (1, 3),             -- Auth: Java, REST API
(2, 1), (2, 2),             -- Pipeline: Java, MySQL
(3, 5), (3, 6),             -- Deployment: Docker, AWS
(4, 7), (4, 3),             -- AI API: Python, REST API
(5, 5), (5, 6),             -- Cloud Infra: Docker, AWS
(6, 4),                     -- UI: React
(7, 8), (7, 1),             -- Security: Security, Java
(8, 3), (8, 4);             -- Patient API: REST API, React

-- 8. Insert Resource Allocations with Intentional Viva Conflict Scenarios
INSERT INTO resource_allocations (id, developer_id, project_id, milestone_id, allocation_percentage, start_date, end_date, hourly_cost_snapshot, estimated_hours) VALUES
-- SCENARIO 1: OVER-ALLOCATION CONFLICT on Priya Sharma (Dev 2)
-- Project 1, Milestone 1 (Oct 1 - Oct 20): 60%
(1, 2, 1, 1, 60, '2026-10-01', '2026-10-20', 800.00, 80.0),
-- Project 2, Milestone 5 (Oct 10 - Oct 25): 50%
-- Overlap window Oct 10 - Oct 20: 60% + 50% = 110% peak allocation! (>100% Conflict)
(2, 2, 2, 5, 50, '2026-10-10', '2026-10-25', 800.00, 80.0),

-- SCENARIO 2: LEAVE CONFLICT on Rahul Verma (Dev 3)
-- Rahul allocated to Milestone 2 (Oct 10 - Nov 05), but has approved leave Oct 12 - Oct 16!
(3, 3, 1, 2, 80, '2026-10-10', '2026-11-05', 750.00, 120.0),

-- SCENARIO 3: SKILL-GAP ALERT on Arun Kumar (Dev 1)
-- Milestone 4 requires Python & REST API. Arun has Java & REST API, but lacks Python!
(4, 1, 2, 4, 70, '2026-10-08', '2026-10-28', 600.00, 100.0),

-- Healthy Normal Allocation for Sneha Patel (Dev 4)
(5, 4, 2, 6, 75, '2026-11-01', '2026-12-20', 500.00, 110.0),

-- Healthy Normal Allocation for Vikram Singh (Dev 5)
(6, 5, 3, 7, 60, '2026-10-15', '2026-11-15', 700.00, 70.0);

-- 9. Insert Planned Leave (Rahul Verma has planned leave overlapping Milestone 2)
INSERT INTO developer_availability (id, developer_id, start_date, end_date, reason, status) VALUES
(1, 3, '2026-10-12', '2026-10-16', 'Annual Medical Leave & Recovery', 'APPROVED'),
(2, 4, '2026-12-24', '2026-12-31', 'Year-End Family Vacation', 'APPROVED');

-- 10. Insert Project Budgets
INSERT INTO budgets (id, project_id, total_budget, current_spend, allocated_cost, currency) VALUES
(1, 1, 500000.00, 350000.00, 154000.00, 'INR'),
(2, 2, 800000.00, 280000.00, 155000.00, 'INR'),
(3, 3, 450000.00, 120000.00, 49000.00, 'INR'),
(4, 4, 600000.00, 50000.00, 0.00, 'INR');

-- 10b. Insert Budget Transactions (Rolling-Window Burn-Rate Tracking)
INSERT INTO budget_transactions (id, project_id, amount, transaction_date, description) VALUES
(1, 1, 100000.00, DATE_SUB(CURRENT_DATE, INTERVAL 18 DAY), 'Architecture sprint & core modules development'),
(2, 1, 120000.00, DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY), 'Microservices containerization & database provisioning'),
(3, 1, 130000.00, DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), 'Security compliance audit & API gateway setup'),
(4, 2, 150000.00, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), 'Initial model pipeline training data ingestion'),
(5, 2, 130000.00, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 'GPU cluster compute hours & API serving setup'),
(6, 3, 120000.00, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'HIPAA compliance audit & authentication backend'),
(7, 4, 50000.00, DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), 'IoT edge hardware integration & prototype testing');

-- 11. Insert System-Generated Notifications
INSERT INTO notifications (id, user_id, title, message, type, severity, is_read) VALUES
(1, 1, 'Critical Over-Allocation Detected', 'Priya Sharma is allocated to 110% cumulative load between 2026-10-10 and 2026-10-20 across Project Alpha & Beta.', 'OVER_ALLOCATION', 'CRITICAL', FALSE),
(2, 1, 'Planned Leave Conflict Flagged', 'Rahul Verma has approved leave (2026-10-12 to 2026-10-16) while actively allocated to Milestone Order Processing Pipeline.', 'LEAVE_CONFLICT', 'WARNING', FALSE),
(3, 1, 'Milestone Skill Gap Identified', 'Milestone AI Model Serving REST API requires Python which is not currently mapped to assigned developer Arun Kumar.', 'SKILL_GAP', 'WARNING', FALSE),
(4, 1, 'Budget Exhaustion Alert', 'Project Alpha burn rate has reached ₹17,500/day. Remaining budget ₹150,000 will be exhausted in ~8.5 days.', 'BUDGET_WARNING', 'CRITICAL', FALSE),
(5, 2, 'New Milestone Assignment', 'You have been assigned to Milestone: AI Model Serving REST API at 70% allocation.', 'SYSTEM_ALERT', 'INFO', FALSE),
(6, 2, 'Skill Verified by Track Record', 'Your skill "Java" was auto-promoted to Verified by track record across 3 completed milestones.', 'SYSTEM_ALERT', 'INFO', FALSE);

-- 12. Insert Certifications (Documents Section)
INSERT INTO certifications (id, developer_id, name, issuing_body, issue_date, credential_id, file_name, confidence_score, verified) VALUES
(1, 1, 'AWS Certified Solutions Architect – Associate', 'Amazon Web Services', '2026-05-15', 'AWS-SAA-849204', 'AWS_Solutions_Architect_Certificate.pdf', 0.98, TRUE),
(2, 1, 'Oracle Certified Professional: Java SE 17 Developer', 'Oracle Corporation', '2026-01-20', 'OCP-JAVA-59218', 'Oracle_Java_17_Certificate.pdf', 0.96, TRUE),
(3, 2, 'Meta Certified Front-End Developer', 'Meta / Coursera', '2026-03-10', 'META-FED-39481', 'Meta_Frontend_Specialist.pdf', 0.94, TRUE),
(4, 3, 'Certified Kubernetes Administrator (CKA)', 'Cloud Native Computing Foundation', '2026-04-12', 'CKA-99214', 'CKA_Kubernetes_Admin.pdf', 0.97, TRUE);

-- 13. Insert Leave Requests (Workflow Queue)
INSERT INTO leave_requests (id, developer_id, start_date, end_date, reason, status, reviewed_by, reviewer_comments) VALUES
(1, 1, '2026-11-20', '2026-11-24', 'Attending National Tech Symposium / Conference', 'PENDING', NULL, NULL),
(2, 2, '2026-10-25', '2026-10-28', 'Family Wedding & Personal Leave', 'PENDING', NULL, NULL),
(3, 3, '2026-10-12', '2026-10-16', 'Annual Medical Leave & Recovery', 'APPROVED', 1, 'Approved per project buffer and stand-in plan.'),
(4, 4, '2026-12-24', '2026-12-31', 'Year-End Family Vacation', 'APPROVED', 1, 'Approved for year-end shutdown period.');

-- 14. Insert Skill Endorsements
INSERT INTO skill_endorsements (id, developer_id, skill_id, endorsed_by_user_id) VALUES
(1, 1, 1, 1), -- Manager endorsed Arun for Java
(2, 1, 3, 3), -- Priya endorsed Arun for REST API
(3, 2, 4, 1), -- Manager endorsed Priya for React
(4, 2, 1, 2), -- Arun endorsed Priya for Java
(5, 5, 7, 1); -- Manager endorsed Vikram for Python

-- 15. Insert Profile Audit Logs
INSERT INTO profile_audit_logs (id, developer_id, actor_user_id, action_type, old_value, new_value) VALUES
(1, 1, 1, 'SKILL_APPROVED', 'PENDING', 'VERIFIED_MANAGER: MySQL (Intermediate)'),
(2, 1, 2, 'SKILL_AUTO_VERIFIED', 'PENDING', 'VERIFIED_TRACK_RECORD: Java (Expert - 3 Milestones)'),
(3, 1, 2, 'CERT_UPLOADED', 'None', 'AWS Certified Solutions Architect – Associate (Confidence: 98%)');

-- 16. Insert Task Feedback
INSERT INTO task_feedback (id, milestone_id, developer_id, manager_id, rating, comment) VALUES
(1, 1, 1, 1, 5, 'Exceptional modular design and resilient error handling for OAuth2 pipeline.'),
(2, 4, 5, 1, 5, 'Highly optimized PyTorch tensor runtime with low latency responses.');
