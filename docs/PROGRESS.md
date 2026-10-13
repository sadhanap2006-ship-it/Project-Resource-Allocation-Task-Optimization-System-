# Project Progress Tracker

| Phase | Description | Status | Date | Evidence / Key Output |
| :--- | :--- | :--- | :--- | :--- |
| **0. Inspect** | Workspace inspection, file plan, legacy archive, baseline docs | **DONE** | 2026-09-30 | Existing prototype archived in `legacy_prototype/`, repo structure established, `PROGRESS.md`, `DECISIONS.md`, `.gitignore`, `.env.example` created. |
| **1. Schema** | 18 tables, constraints, indexes, settings seed in `database/01-schema.sql` | **DONE** | 2026-09-30 | Executed cleanly on MySQL 8.0, 18 InnoDB utf8mb4 tables verified, all CHECK/FK/indexes match Section 7, 15 settings rows seeded. |
| **2. Backend skeleton** | Spring Boot 3.x project, Maven POM, package hierarchy, entity mappings, repository layer, response envelopes, global exception handler, validation | **DONE** | 2026-09-30 | Spring Boot 3.3.4, Java 17, 18 entity mappings, 18 Spring Data repositories, ApiResponse envelope, GlobalExceptionHandler, ddl-auto=validate passed against MySQL 8.0, mvn test exit code 0. |
| **3. Security & CRUD** | Spring Security 6 session-based auth, CSRF, RBAC, domain CRUD services & endpoints | **DONE** | 2026-09-30 | Implemented CustomUserDetailsService, BCrypt, SecurityConfig, session auth with HttpSessionSecurityContextRepository, AuthController, AdminController, ResourceController, SkillController, ProjectController, MilestoneController, AllocationController. 10 integration tests passed with 0 failures. |
| **4. Engine core** | Pure calculation engines: CapacityCalculator, ConflictRules, SkillMatcher, BurnoutTracker, SettingsService & comprehensive unit tests | **DONE** | 2026-09-30 | Implemented pure engine snapshot models, CapacityCalculator (Rules R3, R4, R9), SkillMatcher (credits, matching, ranking), BurnoutTracker (ISO week sustained load), ConflictRules (all Section 9.2 types). All 14 Section 16.1 worked example tests passed (33/33 tests passing with 0 errors). |
| **5. Conflict Service & Seed** | ConflictDetectionService (incremental + nightly + on-demand), notifications, `02-data.sql` seeded relative to CURDATE() | PENDING | - | - |
| **6. Budget & Delivery** | BudgetCalculator (EVM, burn rate, runout forecast), DeliveryForecaster, conflict hooks & API endpoints | PENDING | - | - |
| **7. Simulation & PDF** | In-memory what-if scenario simulation engine (zero DB mutation), recommendation logic, OpenPDF export | PENDING | - | - |
| **8. Dashboards & Timeline APIs** | PM & Dev dashboard aggregation endpoints, Gantt timeline, Heatmap matrix data providers | PENDING | - | - |
| **9. Frontend** | Vanilla HTML5/CSS3/ES6+ SPA/multi-page dashboards, Chart.js 4.x integration, modals, filters | PENDING | - | - |
| **10. Scheduler & Polish** | Nightly cron job, notification badge polling, accessibility audit, loading/error states | PENDING | - | - |
| **11. Docker** | Multi-stage Dockerfile, docker-compose.yml, nginx.conf reverse proxy, timezone synchronization | PENDING | - | - |
| **12. Docs & Postman** | System architecture diagrams (Draw.io), ER diagrams, comprehensive Postman collection, README.md | PENDING | - | - |
| **13. Final Audit** | Complete test suite execution, AC-1 through AC-12 checklist verification matrix | PENDING | - | - |
