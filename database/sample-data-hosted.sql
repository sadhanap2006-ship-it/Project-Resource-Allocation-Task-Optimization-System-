-- =====================================================================
--  PROJECT RESOURCE ALLOCATION SYSTEM  -  SAMPLE DATA (hosted database)
--
--  Identical to sample-data.sql, with the "USE" statement removed so it
--  can be loaded into a database a hosting service already created.
--
--  This data is deliberately built so that, the moment you start the
--  application, the dashboard already demonstrates every feature:
--
--    (A) OVER-ALLOCATION CONFLICT  -> Rahul is at 110 % (60 % + 50 %)
--    (B) SKILL GAP                 -> "Cloud Deployment" needs Azure,
--                                     and NO developer in the system has it
--    (C) LEAVE CONFLICT            -> Priya is 80 % allocated during her leave
--    (D) BUDGET WARNING            -> E-Commerce project is 90 % spent
--    (E) SUCCESSFUL ALLOCATION     -> Arun, 60 %, perfect skill match, no conflict
--    (F) BUDGET FORECAST EXAMPLE   -> Hospital project matches the specification:
--                                     500,000 budget / 300,000 spent /
--                                     10,000 per day  ->  20 days remaining
--
--  Reference "today" used when building this data: 2026-09-18
-- =====================================================================


-- ---------------------------------------------------------------------
-- SKILLS
-- ---------------------------------------------------------------------
INSERT INTO skills (id, name, category) VALUES
 (1, 'Java',             'Backend'),
 (2, 'Spring Boot',      'Backend'),
 (3, 'MySQL',            'Database'),
 (4, 'Python',           'Backend'),
 (5, 'Data Analysis',    'Analytics'),
 (6, 'Machine Learning', 'Analytics'),
 (7, 'JavaScript',       'Frontend'),
 (8, 'HTML',             'Frontend'),
 (9, 'CSS',              'Frontend'),
 (10,'AWS',              'Cloud'),
 (11,'Azure',            'Cloud');      -- nobody has this one -> skill gap demo

-- ---------------------------------------------------------------------
-- DEVELOPERS (resource profiles)
-- ---------------------------------------------------------------------
INSERT INTO developers (id, name, email, role, hourly_cost, availability_percentage, status) VALUES
 (1, 'Arun',    'arun@sist.edu',    'Backend Developer',    500.00, 100, 'ACTIVE'),
 (2, 'Priya',   'priya@sist.edu',   'Data Analyst',         650.00, 100, 'ACTIVE'),
 (3, 'Rahul',   'rahul@sist.edu',   'Frontend Developer',   450.00, 100, 'ACTIVE'),
 (4, 'Divya',   'divya@sist.edu',   'Backend Developer',    550.00, 100, 'ACTIVE'),
 (5, 'Karthik', 'karthik@sist.edu', 'Cloud Engineer',       700.00, 100, 'ACTIVE');

-- ---------------------------------------------------------------------
-- DEVELOPER SKILLS
-- ---------------------------------------------------------------------
INSERT INTO developer_skills (developer_id, skill_id, proficiency_level) VALUES
 -- Arun: Java, Spring Boot, MySQL
 (1, 1, 'EXPERT'), (1, 2, 'EXPERT'), (1, 3, 'INTERMEDIATE'),
 -- Priya: Python, Data Analysis, Machine Learning
 (2, 4, 'EXPERT'), (2, 5, 'EXPERT'), (2, 6, 'INTERMEDIATE'),
 -- Rahul: JavaScript, HTML, CSS
 (3, 7, 'EXPERT'), (3, 8, 'EXPERT'), (3, 9, 'EXPERT'),
 -- Divya: Java, MySQL
 (4, 1, 'INTERMEDIATE'), (4, 3, 'EXPERT'),
 -- Karthik: Java, Spring Boot, AWS
 (5, 1, 'INTERMEDIATE'), (5, 2, 'INTERMEDIATE'), (5, 10, 'EXPERT');

-- ---------------------------------------------------------------------
-- USERS (demo logins for the role-based dashboards)
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, role, developer_id) VALUES
 ('manager', 'manager123', 'PROJECT_MANAGER', NULL),
 ('arun',    'arun123',    'DEVELOPER',       1),
 ('priya',   'priya123',   'DEVELOPER',       2),
 ('rahul',   'rahul123',   'DEVELOPER',       3),
 ('divya',   'divya123',   'DEVELOPER',       4),
 ('karthik', 'karthik123', 'DEVELOPER',       5);

-- ---------------------------------------------------------------------
-- PROJECTS
-- ---------------------------------------------------------------------
INSERT INTO projects (id, name, description, start_date, end_date, budget, status) VALUES
 (1, 'Hospital Management Portal',
     'Patient records, appointments and billing portal for a multi-speciality hospital.',
     '2026-09-01', '2026-12-31', 500000.00, 'IN_PROGRESS'),
 (2, 'E-Commerce Analytics Platform',
     'Sales and customer analytics dashboard for an online store.',
     '2026-09-01', '2026-11-30', 400000.00, 'AT_RISK'),
 (3, 'College ERP Migration',
     'Migration of the legacy college ERP database to a new system.',
     '2026-10-01', '2027-01-31', 300000.00, 'PLANNED');

-- ---------------------------------------------------------------------
-- MILESTONES
--   estimated_effort_days is measured in DEVELOPER-DAYS
-- ---------------------------------------------------------------------
INSERT INTO milestones
 (id, project_id, name, description, start_date, end_date, progress_percentage, status, estimated_effort_days) VALUES
 (1, 1, 'Backend API Development', 'REST APIs for patients, doctors and billing.',
      '2026-09-01', '2026-09-30', 40, 'IN_PROGRESS', 20),
 (2, 1, 'Frontend Development',    'Patient portal screens.',
      '2026-09-15', '2026-10-20', 10, 'IN_PROGRESS', 25),
 (3, 1, 'Cloud Deployment',        'Deploy the portal to the hospital cloud account.',
      '2026-11-01', '2026-11-30',  0, 'PLANNED',     15),
 (4, 2, 'Data Pipeline',           'Collect and clean daily sales data.',
      '2026-09-05', '2026-10-05', 35, 'IN_PROGRESS', 22),
 (5, 2, 'Reporting Dashboard',     'Charts and reports for the sales team.',
      '2026-09-10', '2026-10-15', 15, 'IN_PROGRESS', 18),
 (6, 3, 'Requirement Analysis',    'Study the legacy ERP database.',
      '2026-10-01', '2026-10-20',  0, 'PLANNED',     10),
 (7, 3, 'Data Migration',          'Move legacy data into the new schema.',
      '2026-10-21', '2026-12-15',  0, 'PLANNED',     30);

-- ---------------------------------------------------------------------
-- MILESTONE REQUIRED SKILLS
-- ---------------------------------------------------------------------
INSERT INTO milestone_skills (milestone_id, skill_id) VALUES
 (1, 1), (1, 2), (1, 3),        -- Backend API      : Java, Spring Boot, MySQL
 (2, 7), (2, 8), (2, 9),        -- Frontend         : JavaScript, HTML, CSS
 (3, 10), (3, 11),              -- Cloud Deployment : AWS, Azure  <-- Azure = SKILL GAP
 (4, 4), (4, 5),                -- Data Pipeline    : Python, Data Analysis
 (5, 7), (5, 8), (5, 9),        -- Reporting        : JavaScript, HTML, CSS
 (6, 3),                        -- Requirement      : MySQL
 (7, 1), (7, 3);                -- Data Migration   : Java, MySQL (nobody allocated yet)

-- ---------------------------------------------------------------------
-- RESOURCE ALLOCATIONS
-- ---------------------------------------------------------------------
INSERT INTO resource_allocations
 (id, developer_id, project_id, milestone_id, start_date, end_date, allocation_percentage) VALUES
 -- (E) SUCCESSFUL ALLOCATION: Arun 60 %, has every required skill, no conflict
 (1, 1, 1, 1, '2026-09-01', '2026-09-30', 60),

 -- Divya helps on the same milestone at 50 % (total on the milestone = 110 % capacity)
 (2, 4, 1, 1, '2026-09-01', '2026-09-30', 50),

 -- (A) OVER-ALLOCATION: Rahul 60 % + 50 % on OVERLAPPING dates = 110 %
 (3, 3, 1, 2, '2026-09-15', '2026-10-20', 60),
 (4, 3, 2, 5, '2026-09-10', '2026-10-15', 50),

 -- (C) LEAVE CONFLICT: Priya is 80 % allocated, and is on leave 10-14 September
 (5, 2, 2, 4, '2026-09-05', '2026-10-05', 80),

 -- (B) SKILL GAP: Karthik covers AWS but nobody in the company knows Azure
 (6, 5, 1, 3, '2026-11-01', '2026-11-30', 70),

 -- Non-overlapping example (specification CASE 3): Divya works on the ERP project
 -- in November, which does NOT overlap her September allocation -> no conflict
 (7, 4, 3, 6, '2026-10-21', '2026-11-20', 60);

-- ---------------------------------------------------------------------
-- DEVELOPER LEAVE
-- ---------------------------------------------------------------------
INSERT INTO developer_leave (developer_id, start_date, end_date, reason, status) VALUES
 (2, '2026-09-10', '2026-09-14', 'Family function',  'APPROVED'),  -- (C) conflict
 (1, '2026-10-05', '2026-10-08', 'Personal leave',   'APPROVED'),  -- no conflict
 (5, '2026-12-20', '2026-12-26', 'Annual vacation',  'PLANNED');   -- no conflict

-- ---------------------------------------------------------------------
-- PROJECT BUDGETS
-- ---------------------------------------------------------------------
INSERT INTO project_budgets (id, project_id, total_budget, current_spending) VALUES
 (1, 1, 500000.00, 300000.00),   -- (F) 60 % used  -> forecast example from the spec
 (2, 2, 400000.00, 360000.00),   -- (D) 90 % used  -> CRITICAL budget warning
 (3, 3, 300000.00,      0.00);   --     0 % used   -> "No spending data available"

-- ---------------------------------------------------------------------
-- BUDGET TRANSACTIONS
--
-- Project 1: the first transaction is dated 2026-08-19 and the total is
-- 300,000. Thirty days later (2026-09-18) the burn rate works out to
--     300,000 / 30 days = 10,000 per day
-- and the remaining 200,000 lasts
--     200,000 / 10,000 = 20 days
-- which is exactly CASE 6 of the specification.
-- ---------------------------------------------------------------------
INSERT INTO budget_transactions (project_budget_id, transaction_date, amount, category, description) VALUES
 (1, '2026-08-19', 100000.00, 'Salary', 'Backend team - fortnight 1'),
 (1, '2026-09-02', 120000.00, 'Salary', 'Backend team - fortnight 2'),
 (1, '2026-09-10',  50000.00, 'Cloud',  'Hospital cloud hosting'),
 (1, '2026-09-16',  30000.00, 'Tools',  'Licences and test devices'),

 (2, '2026-08-25', 150000.00, 'Salary', 'Analytics team - month 1'),
 (2, '2026-09-08', 140000.00, 'Salary', 'Analytics team - month 2'),
 (2, '2026-09-15',  70000.00, 'Cloud',  'Data warehouse compute');
 -- Project 3 deliberately has NO transactions (divide-by-zero guard demo)
