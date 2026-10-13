# Project Resource Allocation and Task Optimization System
## Implementation Plan & Technical Architecture

**Course**: SIST CSE AI Capstone Project  
**Author**: Capstone Engineering Team  
**Stack**: Pure Java SE (`HttpServer` + JDBC MySQL 8.0) | HTML5 / CSS3 / JavaScript  

---

## 1. System Overview & Architectural Paradigm
The Project Resource Allocation System is an enterprise resource planning and scheduling engine engineered to solve developer over-allocation, technical skill shortages, unmanaged leave overlap, and project budget burn-rates across concurrent software development streams.

### Architecture Highlights
- **Zero Heavy Frameworks**: Pure Java SE standard library (`com.sun.net.httpserver.HttpServer`, `java.sql.*`). No Spring Boot, no Hibernate/JPA, no Maven, no Gradle.
- **Strict 3-Tier Layering**:
  1. **Presentation / Web Layer**: Vanilla HTML5, CSS3 variables, and vanilla ES6 JavaScript. Embedded directly in the Pure Java HTTP server without external web servers.
  2. **Controller & DTO Layer**: Dedicated REST API route controllers consuming and producing strongly-typed Data Transfer Objects (`backend/src/dto/`), preventing database leak.
  3. **Service & Business Intelligence Layer**: Core algorithms for sweep-line over-allocation detection, rolling-window budget burn-rate forecasting, and isolated in-memory what-if simulations.
  4. **Data Access Layer (DAO)**: Pure JDBC DAO patterns with SQL prepared statements, connection pooling compatibility, and transactional integrity on MySQL 8.0 (3NF Normalized).
  5. **Pure Java PDF Generator**: Standard binary PDF 1.4 output generator (`util.PdfGenerator`) with zero external network or JAR dependencies.

---

## 2. Core Modules & Endpoints

| Area | Controller | Key Endpoints | Implementing Files |
| :--- | :--- | :--- | :--- |
| **Auth** | `AuthController` | `POST /api/login` | `controller/AuthController.java`, `service/AuthService.java` |
| **Resources** | `DeveloperController` | `GET/POST/PUT/DELETE /api/developers` | `controller/DeveloperController.java`, `service/DeveloperService.java` |
| **Projects** | `ProjectController` | `GET/POST/PUT/DELETE /api/projects`, `GET /api/projects/{id}/budget` | `controller/ProjectController.java`, `service/ProjectService.java` |
| **Milestones** | `MilestoneController` | `GET/POST/PUT/DELETE /api/milestones` | `controller/MilestoneController.java`, `service/MilestoneService.java` |
| **Allocations** | `AllocationController` | `GET/POST/DELETE /api/allocations` | `controller/AllocationController.java`, `service/AllocationService.java` |
| **Leaves** | `AvailabilityController` | `GET/POST/DELETE /api/availability` | `controller/AvailabilityController.java`, `service/AvailabilityService.java` |
| **Conflicts** | `DashboardController` | `GET /api/conflicts`, `GET /api/skill-gaps` | `service/ConflictDetectionService.java`, `service/SkillGapService.java` |
| **Budget** | `ProjectController` | `GET /api/projects/{id}/budget` | `service/BudgetService.java`, `dao/BudgetTransactionDAO.java` |
| **What-If** | `SimulationController` | `POST /api/simulations`, `GET /api/simulations/{id}/report` | `service/SimulationService.java`, `util/PdfGenerator.java` |
| **Alerts** | `NotificationController`| `GET/PUT /api/notifications` | `service/NotificationService.java`, `dao/NotificationDAO.java` |

---

## 3. Database Schema Design (MySQL 8.0, 3NF)
- `users`: User identity and role permissions (`PROJECT_MANAGER`, `DEVELOPER`).
- `developers`: Technical profiles, hourly cost, baseline capacity (`availability_percentage`).
- `skills` & `developer_skills`: Technical skills taxonomy and M:N developer associations.
- `projects` & `project_skills`: Software project bounds, budgets, spend, and required skills.
- `milestones` & `milestone_skills`: Project deliverables, effort hours, and required skills.
- `resource_allocations`: Assigned developer percentage per milestone with rate snapshot.
- `developer_availability`: Time-bounded planned vacation and approved leave intervals.
- `budgets`: High-level total budget, current spend, and allocated cost tracking.
- `budget_transactions`: Audit log of spend transactions enabling rolling-window burn rate calculation.
- `what_if_scenarios`: Audit log of applied simulation decisions.
- `notifications`: System-generated critical alerts and schedule warnings.

---

## 4. Test Verification Strategy
An assertion-based unit testing framework is located in `backend/test/` and run via `run_tests.bat`:
1. `AllocationConflictTest`: Validates the sweep-line interval algorithm on overlapping date ranges (e.g. 60% Jan 1-20 + 50% Jan 10-25 = 110% peak conflict Jan 10-20).
2. `LeaveConflictTest`: Validates leave date intersection detection and 0% capacity deduction.
3. `SkillGapTest`: Validates missing competency detection on assignments.
4. `BudgetCalculationTest`: Validates budget utilization, rolling-window burn rate (`recentSpend / elapsedTime`), and zero-spend edge cases.
5. `SimulationTest`: Validates cost delta math, delivery recalculation, and guarantees 0% database mutation during simulation evaluation.

For step-by-step viva defense notes and testing matrices, refer to:
- [docs/DEFINE.md](file:///c:/Users/sadha/.gemini/antigravity-ide/scratch/project-resource-allocation-system/docs/DEFINE.md)
- [docs/DESIGN.md](file:///c:/Users/sadha/.gemini/antigravity-ide/scratch/project-resource-allocation-system/docs/DESIGN.md)
- [docs/API.md](file:///c:/Users/sadha/.gemini/antigravity-ide/scratch/project-resource-allocation-system/docs/API.md)
- [docs/VIVA_GUIDE.md](file:///c:/Users/sadha/.gemini/antigravity-ide/scratch/project-resource-allocation-system/docs/VIVA_GUIDE.md)
- [ACCEPTANCE_CRITERIA_CHECKLIST.md](file:///c:/Users/sadha/.gemini/antigravity-ide/scratch/project-resource-allocation-system/ACCEPTANCE_CRITERIA_CHECKLIST.md)
