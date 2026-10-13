# Acceptance Criteria Checklist (AC-01 through AC-10)

## SIST CSE AI Capstone Project — Final Evaluation Matrix

This formal verification matrix records the operational compliance of each Acceptance Criterion defined in the project specification, including the verified status and implementing source files.

---

| AC ID | Acceptance Criterion & Scenario | Implementing Files | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| **AC-01** | **User Authentication & Role Dashboards**<br>Supports distinct login sessions for Project Manager (`manager@company.com`) and Developer (`arun@company.com`), routing to respective views with role-based UI controls. | `backend/src/controller/AuthController.java`<br>`backend/src/service/AuthService.java`<br>`frontend/js/script.js` | `POST /api/login` returning 200 OK with `PROJECT_MANAGER` / `DEVELOPER` role and appropriate frontend view activation. | **PASS** |
| **AC-02** | **Over-Allocation Conflict Detection (>100%)**<br>Detects when cumulative developer allocation exceeds 100% across concurrent milestones (e.g. Priya Sharma 60% on M1 + 50% on M5 = 110% peak overlap). | `backend/src/service/ConflictDetectionService.java`<br>`backend/test/AllocationConflictTest.java` | Sweep-line boundary algorithm flags `OVER_ALLOCATION_CONFLICT` with peak load `110%` and persists system notification. | **PASS** |
| **AC-03** | **Approved Leave Conflict Detection**<br>Detects when an assigned developer has approved planned leave intersecting active milestone dates (e.g. Rahul Verma leave Oct 12-16 on M2 running Oct 10 - Nov 5). | `backend/src/service/ConflictDetectionService.java`<br>`backend/src/service/AvailabilityService.java`<br>`backend/test/LeaveConflictTest.java` | Inclusive date overlap logic detects intersection, enforces 0% effective capacity during leave, flags `LEAVE_CONFLICT`. | **PASS** |
| **AC-04** | **Skill-Gap Identification**<br>Compares milestone required skills against assigned developer competencies and flags missing competencies (e.g. Arun Kumar assigned to M4 lacking `Python`). | `backend/src/service/SkillGapService.java`<br>`backend/test/SkillGapTest.java` | Relational junction comparison flags `SKILL_GAP_ALERT` listing specific missing skills (`Python`) and remediation recommendation. | **PASS** |
| **AC-05** | **What-If Simulation (Zero Database Mutation)**<br>Simulates resource swap (e.g. Priya Sharma ₹800/hr swapped with Arun Kumar ₹600/hr), computing cost variance (-₹16,000), delivery delta, and conflict impact in-memory with zero DB mutation. | `backend/src/service/SimulationService.java`<br>`backend/test/SimulationTest.java` | `POST /api/simulations` returns verdict `CRITICAL_RISK` and ₹16,000 savings; SQL query confirms zero rows modified in `resource_allocations`. | **PASS** |
| **AC-06** | **What-If Swap Application**<br>Allows Project Manager to officially commit simulated swap to the real schedule, persisting update and dynamically recalculating conflicts. | `backend/src/service/SimulationService.java`<br>`backend/src/controller/SimulationController.java` | `POST /api/simulations/{id}/apply` safely reassigns developer in MySQL and dynamically refreshes conflict badges. | **PASS** |
| **AC-07** | **Budget Burn-Rate & Exhaustion Forecast**<br>Computes daily spending velocity using rolling-window transactions (`burnRate = recentSpend / elapsedTime`) and projects budget exhaustion countdown. | `backend/src/service/BudgetService.java`<br>`backend/src/dao/BudgetTransactionDAO.java`<br>`backend/test/BudgetCalculationTest.java` | `GET /api/projects/1/budget` computes burn rate, days until exhaustion (~8.5 days), and handles zero-spend fallback gracefully. | **PASS** |
| **AC-08** | **Developer Portal Restricted View**<br>Restricted view displaying assigned milestones, personal workload percentage, approved leaves, and relevant notifications without PM edit actions. | `backend/src/controller/DashboardController.java`<br>`frontend/js/script.js` | `GET /api/dashboard/developer/1` returns Arun Kumar's exact 70% load, Milestone 4 details, and personal alert messages. | **PASS** |
| **AC-09** | **Real-Time Dynamic Allocation Bar & Heatmap**<br>Interactive capacity sliders recalculate load dynamically with color coding (Green <=80%, Amber 81-100%, Crimson >100%) and render weekly heatmap. | `frontend/js/script.js`<br>`frontend/css/style.css` | DOM input listeners trigger instant recalculation; heatmap colors each week according to developer workload. | **PASS** |
| **AC-10** | **Academic Viva Quick Scenarios & Report Export**<br>Single-click evaluation buttons (Scenario A, B, C) and pure Java downloadable PDF report export for simulated what-if outcomes. | `backend/src/util/PdfGenerator.java`<br>`backend/src/controller/SimulationController.java`<br>`frontend/js/script.js` | Evaluator buttons filter and animate corresponding conflict cards; "Export Report" downloads a valid binary PDF document. | **PASS** |

---

## Final Verification Summary
- **Total Acceptance Criteria**: 10
- **Criteria Passed**: 10 (100%)
- **Criteria Failed**: 0
- **Compliance Status**: Fully Compliant
