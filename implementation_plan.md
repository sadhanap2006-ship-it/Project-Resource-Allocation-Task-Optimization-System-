# Implementation Plan — Project Resource Allocation System

**SIST CSE AI Capstone Project**

This document is written so that every design decision can be explained in a viva
using only HTML5, CSS3, JavaScript, Java, Spring Boot and MySQL.

---

## 0. Inspection Result (Phase 1)

Before writing any code the repository was inspected:

| Check | Result |
|-------|--------|
| Existing files | Only `README.md` (one line) and `.git` |
| Java configured? | No project code, but **JDK 21** is installed |
| Maven available? | Yes, **Apache Maven 3.9.11** |
| Spring Boot configured? | No — created from scratch in this plan |
| MySQL configured? | No `my.cnf` / no schema present — created in `database/` |
| Useful code to preserve | None (nothing was deleted) |

Conclusion: this is a green-field project. Everything below is new.

---

## 1. Architecture

### 1.1 Layered architecture

The application uses the classic **3-tier layered architecture**. There are no
microservices, no message queues and no extra design patterns.

```
┌──────────────────────────────────────────────────────┐
│  PRESENTATION TIER                                   │
│  frontend/  →  HTML5 + CSS3 + Vanilla JavaScript     │
│  Talks to the backend with the Fetch API (JSON)      │
└───────────────────────┬──────────────────────────────┘
                        │  HTTP  (JSON over REST)
┌───────────────────────▼──────────────────────────────┐
│  APPLICATION TIER  (Java + Spring Boot)              │
│                                                      │
│   controller/   @RestController — receives requests  │
│        ↓                                             │
│   service/      business rules live here             │
│        ↓                                             │
│   repository/   Spring Data JPA interfaces           │
│                                                      │
│   entity/  dto/  exception/  config/                 │
└───────────────────────┬──────────────────────────────┘
                        │  JDBC
┌───────────────────────▼──────────────────────────────┐
│  DATA TIER  —  MySQL 8.0                             │
└──────────────────────────────────────────────────────┘
```

### 1.2 Why each layer exists (viva answer)

* **Controller** — only knows about HTTP. Reads the request, validates the DTO,
  calls a service, returns a DTO. It contains *no* business logic.
* **Service** — contains all the rules: over-allocation detection, skill gaps,
  leave conflicts, budget maths, forecasting, simulation. This is the part of the
  project that is actually "the project".
* **Repository** — Spring Data JPA interface. We only declare method names such as
  `findByDeveloperId(...)` and Spring writes the SQL for us.
* **Entity** — a Java class mapped to one MySQL table with JPA annotations.
* **DTO** — the object shape sent to / received from the browser. Keeping DTOs
  separate from entities means we never accidentally expose database internals.

### 1.3 Folder structure

```
project-resource-allocation-system/
├── implementation_plan.md
├── README.md
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/sist/rms/
│       │   ├── ResourceAllocationApplication.java
│       │   ├── config/        (CORS + static frontend serving)
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   └── exception/
│       ├── main/resources/application.properties
│       └── test/java/com/sist/rms/service/   (JUnit 5 tests)
├── frontend/
│   ├── index.html  dashboard.html  projects.html  developers.html
│   ├── allocations.html  conflicts.html  simulator.html
│   ├── timeline.html  budget.html  developer-dashboard.html
│   ├── css/style.css
│   └── js/api.js  dashboard.js  projects.js  developers.js
│           allocations.js  conflicts.js  simulator.js
│           timeline.js  budget.js  developer-dashboard.js
├── database/
│   ├── schema.sql
│   └── sample-data.sql
├── docs/
│   ├── architecture.md
│   ├── database-design.md
│   └── api-documentation.md
└── postman/
    └── Project-Resource-Allocation-System.postman_collection.json
```

---

## 2. Database Tables

MySQL 8.0, database name `resource_allocation_db`, engine InnoDB, charset utf8mb4.

### 2.1 `users`
Login accounts and role-based dashboards.

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| password | VARCHAR(100) | NOT NULL |
| role | ENUM('PROJECT_MANAGER','DEVELOPER') | NOT NULL |
| developer_id | BIGINT | FK → developers(id), NULL for a manager |

### 2.2 `developers`  (the "resource profile")

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| role | VARCHAR(80) | NOT NULL (e.g. "Backend Developer") |
| hourly_cost | DECIMAL(10,2) | NOT NULL, >= 0 |
| availability_percentage | INT | NOT NULL, 0–100, default 100 |
| status | ENUM('ACTIVE','INACTIVE','ON_LEAVE') | NOT NULL, default ACTIVE |

### 2.3 `skills`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(80) | NOT NULL, UNIQUE |
| category | VARCHAR(80) | NULL (e.g. "Backend", "Cloud") |

### 2.4 `developer_skills`  (join table, many-to-many)

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| developer_id | BIGINT | NOT NULL, FK → developers(id) ON DELETE CASCADE |
| skill_id | BIGINT | NOT NULL, FK → skills(id) |
| proficiency_level | ENUM('BEGINNER','INTERMEDIATE','EXPERT') | NOT NULL |
| | | UNIQUE (developer_id, skill_id) |

### 2.5 `projects`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(150) | NOT NULL |
| description | VARCHAR(1000) | NULL |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL (must be >= start_date) |
| budget | DECIMAL(14,2) | NOT NULL, >= 0 |
| status | ENUM('PLANNED','IN_PROGRESS','COMPLETED','AT_RISK') | NOT NULL |

### 2.6 `milestones`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| project_id | BIGINT | NOT NULL, FK → projects(id) ON DELETE CASCADE |
| name | VARCHAR(150) | NOT NULL |
| description | VARCHAR(1000) | NULL |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL |
| progress_percentage | INT | NOT NULL, 0–100, default 0 |
| status | ENUM('PLANNED','IN_PROGRESS','COMPLETED','DELAYED','AT_RISK') | NOT NULL |
| estimated_effort_days | INT | NOT NULL, > 0 — planned effort in **developer-days** |

`estimated_effort_days` is what makes the delivery-date formula possible (§5.6).

### 2.7 `milestone_skills`  (skills a milestone requires)

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| milestone_id | BIGINT | NOT NULL, FK → milestones(id) ON DELETE CASCADE |
| skill_id | BIGINT | NOT NULL, FK → skills(id) |
| | | UNIQUE (milestone_id, skill_id) |

### 2.8 `resource_allocations`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| developer_id | BIGINT | NOT NULL, FK → developers(id) |
| project_id | BIGINT | NOT NULL, FK → projects(id) |
| milestone_id | BIGINT | NULL, FK → milestones(id) |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL |
| allocation_percentage | INT | NOT NULL, 1–100 |

### 2.9 `developer_leave`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| developer_id | BIGINT | NOT NULL, FK → developers(id) ON DELETE CASCADE |
| start_date | DATE | NOT NULL |
| end_date | DATE | NOT NULL |
| reason | VARCHAR(255) | NULL |
| status | ENUM('PLANNED','APPROVED','CANCELLED') | NOT NULL, default APPROVED |

### 2.10 `project_budgets`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| project_id | BIGINT | NOT NULL, UNIQUE, FK → projects(id) ON DELETE CASCADE |
| total_budget | DECIMAL(14,2) | NOT NULL, >= 0 |
| current_spending | DECIMAL(14,2) | NOT NULL, >= 0, default 0 |

### 2.11 `budget_transactions`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| project_budget_id | BIGINT | NOT NULL, FK → project_budgets(id) ON DELETE CASCADE |
| transaction_date | DATE | NOT NULL |
| amount | DECIMAL(14,2) | NOT NULL, > 0 |
| category | VARCHAR(80) | NULL (e.g. "Salary", "Cloud") |
| description | VARCHAR(255) | NULL |

`project_budgets.current_spending` is kept as the running total of its transactions
so the dashboard does not need a SUM on every page load.

### 2.12 `notifications`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| type | ENUM('OVER_ALLOCATION','SKILL_GAP','LEAVE_CONFLICT','BUDGET_RISK','MILESTONE_DELAY','MILESTONE_DEADLINE') | NOT NULL |
| severity | ENUM('INFO','WARNING','CRITICAL') | NOT NULL |
| message | VARCHAR(500) | NOT NULL |
| related_entity_type | VARCHAR(50) | NULL ("DEVELOPER", "PROJECT"…) |
| related_entity_id | BIGINT | NULL |
| developer_id | BIGINT | NULL — set when the notice is for one developer |
| is_read | BOOLEAN | NOT NULL, default FALSE |
| created_at | DATETIME | NOT NULL |

### 2.13 `simulation_scenarios`

Saved what-if scenarios. **Nothing is written here unless the manager presses Save.**

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK |
| scenario_name | VARCHAR(150) | NOT NULL |
| project_id | BIGINT | NOT NULL, FK → projects(id) |
| milestone_id | BIGINT | NULL, FK → milestones(id) |
| current_developer_id | BIGINT | NOT NULL, FK → developers(id) |
| new_developer_id | BIGINT | NOT NULL, FK → developers(id) |
| current_allocation_percentage / simulated_allocation_percentage | INT | NOT NULL, 1–100 |
| current_cost / simulated_cost | DECIMAL(14,2) | NOT NULL |
| current_skill_match / simulated_skill_match | INT | NOT NULL (0–100 %) |
| current_conflicts / simulated_conflicts | INT | NOT NULL |
| current_delivery_date / simulated_delivery_date | DATE | NULL |
| budget_impact | DECIMAL(14,2) | NOT NULL (simulated − current) |
| created_at | DATETIME | NOT NULL |

---

## 3. Relationships

```
users            N ──── 1  developers          (a developer may have a login)

developers       1 ──── N  developer_skills  N ──── 1  skills
developers       1 ──── N  resource_allocations
developers       1 ──── N  developer_leave

projects         1 ──── N  milestones
projects         1 ──── N  resource_allocations
projects         1 ──── 1  project_budgets     1 ──── N  budget_transactions
projects         1 ──── N  simulation_scenarios

milestones       1 ──── N  milestone_skills  N ──── 1  skills
milestones       1 ──── N  resource_allocations
```

Two many-to-many relationships (`developer_skills`, `milestone_skills`) are
modelled as explicit join **entities**, not as JPA `@ManyToMany`, because the join
row carries extra data (proficiency) and because an explicit table is far easier
to show and explain in a viva.

---

## 4. Backend Structure

```
com.sist.rms
├── ResourceAllocationApplication.java     main() — starts Spring Boot
├── config/
│   └── WebConfig.java                     CORS + serves the frontend/ folder
├── entity/       (13 classes, one per table)
├── repository/   (13 interfaces extending JpaRepository)
├── dto/          request + response objects
├── exception/
│   ├── ResourceNotFoundException.java     → HTTP 404
│   ├── ValidationException.java           → HTTP 400
│   └── GlobalExceptionHandler.java        @RestControllerAdvice
├── service/
│   ├── DeveloperService, SkillService, ProjectService, MilestoneService
│   ├── AllocationService        creates allocations, guards the rules
│   ├── ConflictService          over-allocation + leave conflicts
│   ├── SkillGapService          skill-gap detection
│   ├── LeaveService
│   ├── BudgetService            budget maths + forecast
│   ├── UtilizationService       resource utilization table
│   ├── TimelineService
│   ├── NotificationService      generates + stores notifications
│   ├── SimulationService        what-if engine (read-only by default)
│   ├── DeliveryEstimateService  the delivery-date formula
│   ├── DashboardService         manager + developer dashboards
│   └── ReportService            PDF export (OpenPDF)
└── controller/   (one @RestController per area)
```

---

## 5. Business Rules

### 5.1 Date overlap

Two date ranges **overlap** when:

```
A.start <= B.end   AND   A.end >= B.start
```

Every rule below uses this single definition.

### 5.2 Over-allocation detection (the timeline sweep)

Summing all of a developer's allocations is *wrong*, because allocations at
different times never compete. Instead:

1. Take every allocation of that developer.
2. Collect all **boundary dates**: every `start_date`, and every `end_date + 1 day`.
3. Sort them. Consecutive boundaries form **segments** of time in which the set of
   active allocations never changes.
4. For each segment, sum `allocation_percentage` of the allocations active in it.
5. If `sum > developer.availability_percentage` → **OVER-ALLOCATION CONFLICT** for
   that segment. Adjacent conflicting segments with the same total are merged so
   the message reads as one date range.

This gives exactly the behaviour required by the specification:

| Case | Project A | Project B | Dates | Total | Result |
|---|---|---|---|---|---|
| 1 | 60 % | 50 % | same | 110 % | CONFLICT |
| 2 | 60 % | 40 % | same | 100 % | OK |
| 3 | 60 % | 50 % | different | never simultaneous | OK |

### 5.3 Leave conflict

For each allocation, find the developer's leave rows (status ≠ CANCELLED) whose
dates overlap the allocation. Every overlap produces a **LEAVE CONFLICT**, because
effective availability during leave is 0 %.

### 5.4 Skill-gap detection

For a milestone:

```
required  = skills listed in milestone_skills
covered   = union of the skills of every developer allocated to that milestone
missing   = required − covered
```

* `missing` is non-empty → **SKILL GAP**, listing the missing skills.
* For each missing skill, look for *any* ACTIVE developer in the system who has it.
  * If somebody has it → suggest them ("Arun has AWS").
  * If nobody has it → `"Skill Gap: No available developer has this skill."`

When an allocation is created for a developer who is missing a required skill of
that milestone, the API still creates the allocation **but** returns a `warnings`
list and writes a SKILL_GAP notification — the assignment is never *silent*.

### 5.5 Budget rules

```
remaining_budget  = total_budget − current_spending
utilization (%)   = (current_spending / total_budget) × 100
```

Guards:
* `total_budget = 0` → show `"No spending data available"`, never divide by zero.
* `current_spending = 0` → show `"No spending data available"` for burn rate and
  forecast.

Warning thresholds used by the dashboard and notifications:
* utilization ≥ 90 % → **CRITICAL** budget warning
* utilization ≥ 75 % → **WARNING**

### 5.6 Budget forecast (simple, deterministic — *not* AI)

```
days_elapsed  = days from the first budget transaction date to today (min 1)
burn_rate     = current_spending / days_elapsed          (₹ per day)
days_left     = remaining_budget / burn_rate             (rounded down)
exhaustion    = today + days_left
```

Worked example from the specification:

```
total  ₹500,000     spending ₹300,000     remaining ₹200,000
burn rate ₹10,000/day  →  200,000 / 10,000 = 20 days
```

Message shown:
> "At the current burn rate, the project is estimated to exhaust its budget in
> approximately 20 days."

This is plain arithmetic on the current burn rate — it is **not** a prediction
made by any AI or machine-learning model, and the UI says so.

### 5.7 Delivery-date formula

Every milestone stores `estimated_effort_days` — how many **developer-days** of
work it needs (e.g. 20 developer-days = one full-time developer for 20 days).

```
capacity        = (sum of allocation_percentage of developers on the milestone) / 100
duration_days   = ceil( estimated_effort_days / capacity )
delivery_date   = milestone.start_date + duration_days − 1
```

* Capacity goes **down** → duration goes **up** → delivery is later.
* Capacity goes **up**   → duration goes **down** → delivery is earlier.
* `capacity = 0` → "Cannot estimate — no developer allocated."

Example: effort 20 developer-days.
* One developer at 100 % → capacity 1.0 → 20 days.
* One developer at 50 %  → capacity 0.5 → 40 days.
* Two developers at 100 % → capacity 2.0 → 10 days.

### 5.8 Cost formula

```
working_days = number of Mon–Fri days between start_date and end_date (inclusive)
hours        = working_days × 8 × (allocation_percentage / 100)
cost         = hours × developer.hourly_cost
```

### 5.9 Skill-match percentage

```
skill_match (%) = (required skills the developer has / total required skills) × 100
```
If the milestone requires no skills, skill match is reported as 100 %.

### 5.10 What-if simulation rules

* The simulator **reads** data, computes the hypothetical numbers in memory and
  returns them. It performs **no** INSERT / UPDATE / DELETE.
* `POST /api/simulations/run` → calculate and return (nothing saved).
* `POST /api/simulations`     → save the scenario, only when the user asks.
* Estimated delivery impact = simulated delivery date − current delivery date,
  using §5.7 with the replacement developer's allocation percentage.

### 5.11 Validation rules

| Rule | Message |
|---|---|
| Blank name | "Name is required" |
| Invalid email | "Email must be a valid email address" |
| Duplicate email | "A developer with this email already exists" |
| Negative hourly cost | "Hourly cost cannot be negative" |
| Negative budget | "Budget cannot be negative" |
| Allocation < 1 or > 100 | "Allocation percentage must be between 1 and 100" |
| end_date < start_date | "End date cannot be before start date" |
| Leave end < leave start | "Leave end date cannot be before leave start date" |
| Allocation outside project dates | "Allocation dates must be inside the project dates" |
| Unknown project / developer / milestone id | "Project not found with id 9" (HTTP 404) |

All errors come back in one consistent JSON shape:

```json
{ "timestamp": "2025-09-18T10:00:00", "status": 400,
  "error": "Validation Failed",
  "messages": ["Allocation percentage must be between 1 and 100"] }
```

---

## 6. REST API Endpoints

Base URL: `http://localhost:8080`

### Projects
| Method | Path |
|---|---|
| GET | `/api/projects` |
| POST | `/api/projects` |
| GET | `/api/projects/{id}` |
| PUT | `/api/projects/{id}` |
| DELETE | `/api/projects/{id}` |
| GET | `/api/projects/{id}/timeline` |

### Developers / Skills / Leave
| Method | Path |
|---|---|
| GET POST | `/api/developers` |
| GET PUT DELETE | `/api/developers/{id}` |
| GET | `/api/developers/{id}/dashboard` |
| POST | `/api/developers/{id}/skills` |
| DELETE | `/api/developers/{id}/skills/{skillId}` |
| GET POST | `/api/skills` |
| GET POST | `/api/leaves` |
| DELETE | `/api/leaves/{id}` |

### Milestones
| Method | Path |
|---|---|
| GET POST | `/api/milestones` (`?projectId=` filter) |
| GET PUT DELETE | `/api/milestones/{id}` |
| POST | `/api/milestones/{id}/skills` |
| DELETE | `/api/milestones/{id}/skills/{skillId}` |

### Allocations / Conflicts / Utilization
| Method | Path |
|---|---|
| GET POST | `/api/allocations` |
| PUT DELETE | `/api/allocations/{id}` |
| GET | `/api/conflicts` |
| GET | `/api/skill-gaps` |
| GET | `/api/utilization` |

### Budget
| Method | Path |
|---|---|
| GET | `/api/projects/{id}/budget` |
| GET | `/api/projects/{id}/budget/forecast` |
| GET POST | `/api/projects/{id}/budget/transactions` |

### Notifications / Dashboard / Simulations
| Method | Path |
|---|---|
| GET | `/api/notifications` |
| POST | `/api/notifications/refresh` (re-scan the system) |
| PUT | `/api/notifications/{id}/read` |
| GET | `/api/dashboard/summary` |
| POST | `/api/simulations/run` — calculate only, **saves nothing** |
| GET POST | `/api/simulations` — list / save a scenario |
| GET | `/api/simulations/{id}` |
| GET | `/api/simulations/{id}/report` — PDF download |
| DELETE | `/api/simulations/{id}` |

The specification listed `POST /api/simulations/{id}/run`; because a simulation
must be runnable *before* it is saved, the run endpoint is `POST
/api/simulations/run` and saving is a separate `POST /api/simulations`. The
specification explicitly allows adjusting endpoint names.

---

## 7. Frontend Pages

| File | Purpose |
|---|---|
| `index.html` | Landing / role chooser: Project Manager or Developer |
| `dashboard.html` | Project Manager dashboard (counts, utilization bars, conflicts, budget, notifications) |
| `projects.html` | Project CRUD + milestone list |
| `developers.html` | Developer CRUD, skills, leave |
| `allocations.html` | Create / edit / delete allocations |
| `conflicts.html` | Over-allocation, leave conflicts, skill gaps |
| `budget.html` | Budget table, burn rate, forecast, transactions |
| `timeline.html` | Project → milestone timeline bars |
| `simulator.html` | What-if resource swap + Save + PDF export |
| `developer-dashboard.html` | The simpler developer view |

Shared JavaScript:
* `js/api.js` — one tiny wrapper around `fetch()` plus shared helpers
  (error display, date formatting, currency formatting, nav bar).
* One `js/<page>.js` per page.

Charts are plain HTML `<div>` elements sized with CSS percentage widths
(utilization bars, budget bars, timeline bars) and CSS-coloured status badges.
No charting library is used.

---

## 8. Testing Plan

### 8.1 JUnit tests (`backend/src/test/java`)
Plain unit tests on the service classes — no database needed.

| Test | Covers |
|---|---|
| `ConflictServiceTest` | Cases 1, 2, 3 and 5 from the specification |
| `BudgetServiceTest` | Case 6, zero-budget and zero-spending guards |
| `SkillGapServiceTest` | Case 4 ("no available developer has this skill") |
| `DeliveryEstimateServiceTest` | Capacity ↑ / ↓ changes the duration correctly |

Run with `mvn test`.

### 8.2 Postman collection (`postman/`)
Folders: Projects, Developers, Milestones, Allocations, Conflicts, Budgets,
Notifications, Simulations. Each folder contains a success request, an invalid
request (expect 400) and a missing-record request (expect 404).

### 8.3 Sample-data acceptance checks
After loading `database/sample-data.sql` the UI must show, without any manual
setup, at least one of each: over-allocation conflict, skill gap, leave conflict,
budget warning, and a clean successful allocation.

---

## 9. Development Phases

| Phase | Work | Status |
|---|---|---|
| 1 | Inspect project | done |
| 2 | Architecture + this plan | done |
| 3 | MySQL schema + sample data | |
| 4 | Spring Boot skeleton, entities, repositories | |
| 5 | Resource allocation logic | |
| 6 | Conflict detection | |
| 7 | Skill-gap detection | |
| 8 | Leave / availability | |
| 9 | Budget calculation + forecast | |
| 10 | REST controllers | |
| 11 | HTML/CSS/JS frontend | |
| 12 | Connect frontend with Fetch API | |
| 13 | Manager + developer dashboards | |
| 14 | What-if simulator | |
| 15 | Notifications | |
| 16 | PDF report export | |
| 17 | Postman testing | |
| 18 | Bug fixing | |
| 19 | README + architecture docs | |

---

## 10. Dependencies Outside the Named Stack

Only **one** library is used that is not Spring Boot / MySQL:

**OpenPDF** (`com.github.librepdf:openpdf`)

1. **Why it is needed** — the specification asks for a PDF export of the what-if
   report. Java has no PDF writer in its standard library.
2. **What it does** — it creates a PDF document from Java code: you add
   paragraphs and tables and it writes the `.pdf` bytes. We use about 60 lines of it.
3. **Can the project work without it?** — Yes. The simulator page also has a
   "Print / Save as PDF" button that uses the browser's own print dialog with a
   print-friendly CSS stylesheet. OpenPDF only adds a proper server-side
   `GET /api/simulations/{id}/report` download.

OpenPDF was chosen over Apache PDFBox because PDFBox is a low-level library where
you position text by coordinates, while OpenPDF has a simple
`document.add(new Paragraph("..."))` API that a student can read and explain.
