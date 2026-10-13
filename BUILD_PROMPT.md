# BUILD PROMPT — Project Resource Allocation System

SIST CSE Capstone · Specification version 1.0 · 30 September 2026

You are building a complete, working full-stack web application from this specification. This file is the single source of truth. The always-on rules in `.agent/rules/project-rules.md` apply to everything you do.

---

## 0. How to use this document

- Read the whole document once before Phase 0.
- Build strictly phase by phase (Section 18). After each phase, run the `verify-phase` workflow, report, and wait for the user.
- Section numbers are referenced from code comments and reports (for example "Rule R4", "Section 9.3").
- Section 20 (Clarifications) resolves ambiguities and wins over earlier sections.
- If anything is still unclear, ask. Do not guess.

---

## 1. Mission and context

**Problem statement.** IT project managers struggle to allocate developers, lab hardware and cloud compute resources across overlapping projects, leading to developer burnout, skill mismatches and severe budget overruns.

**Recommended approach.** A smart project resource scheduling matrix that matches developer skillsets and availability percentages to project milestones, monitors budget burn-down rates, and detects over-allocation conflicts via timeline visualization.

**Given acceptance criteria.**

1. AC1: Resource profiles capture skill tags, hourly cost and percentage bandwidth allocation across active projects.
2. AC2: The conflict detection engine flags any developer allocated above 100% capacity across simultaneous milestones.
3. AC3: Project managers can simulate resource swaps and see instant recalculations of delivery dates and costs.
4. AC4: An interactive timeline dashboard displays project health, milestone progress and resource utilization heatmaps.

**Team extensions (AC5–AC12).** Skill-gap alerts and candidate ranking; budget burn-down and forecast; leave and maintenance reduce capacity; in-app notifications; what-if PDF export; role-based dashboards enforced by the server; hardware and cloud handled by the same conflict logic; burnout (sustained load) warning.

**What the system is.** One web application where a project manager (PM) plans developers, lab hardware and cloud compute across many projects. It warns before any resource is overloaded, before a needed skill is missing, and before a budget runs out, and it lets the PM test a change safely ("what if") before applying it.

**What the system is NOT.** Not AI or machine learning: every calculation is a deterministic, explainable formula. Not a payroll, invoicing or HR system. It does not read live cloud billing APIs. It is not multi-tenant.

---

## 2. Roles and permissions

Three roles. Every rule below is enforced on the SERVER for every API call (Section 11). Hiding UI elements is additional, not a replacement.

| Action | PM | DEVELOPER | ADMIN |
| --- | --- | --- | --- |
| Create, edit, archive projects and milestones | Yes | No | No |
| Set milestone skill requirements | Yes | No | No |
| Set project budget; add budget transactions | Yes | No | No |
| Create and edit resources (developer, hardware, cloud) and their skills | Yes | No | Yes |
| Create, edit, delete allocations | Yes | No | No |
| View candidate ranking | Yes | No | No |
| View Conflict Center; acknowledge conflicts | Yes | View own conflicts only | View only |
| Trigger a full conflict scan | Yes | No | Yes |
| View budget and forecast | Yes | No | No |
| Run, save, export simulations | Yes | No | No |
| Request leave | For any developer | Own leave only | No |
| Approve or reject leave; create hardware maintenance | Yes | No | Maintenance only |
| Update milestone progress % | Yes | Only milestones they are allocated to | No |
| View own dashboard (projects, milestones, load, deadlines, leave) | PM dashboard | Developer dashboard | No |
| Receive notifications | Projects they manage | Only about themselves | System notices |
| Manage users, reset passwords, manage the skill list, edit settings | No | No | Yes |

A DEVELOPER user is linked to exactly one DEVELOPER resource (`users.resource_id`). Developer endpoints use `/me` and take the resource ID from the authenticated session, never from the request.

---

## 3. Technology stack (fixed — do not change)

| Layer | Technology |
| --- | --- |
| Frontend | HTML5, CSS3 (CSS Grid, Flexbox), Vanilla JavaScript (ES2020+, Fetch API), Chart.js 4.x (pinned exact version, loaded from a CDN) |
| Backend | Java 17, Spring Boot 3.x (latest stable 3.x from Spring Initializr), Spring Web, Spring Data JPA (Hibernate 6), Bean Validation, Spring Security 6, Spring Boot Actuator, Spring scheduling, OpenPDF (`com.github.librepdf:openpdf`, verified version), Maven |
| Database | MySQL 8.0 |
| Testing | JUnit 5, Mockito, Spring Boot Test, AssertJ (all via `spring-boot-starter-test`) |
| Tools | Postman (collection JSON), Draw.io (diagram files), Git/GitHub |
| Deployment | Docker, Docker Compose, Nginx (`nginx:alpine`), cloud Linux VM |

Not allowed: React, Angular, Vue, Node.js backend, MongoDB, Firebase, Python backend, Lombok (keep code explicit for the viva), MapStruct, any UI component library, any extra charting library.

---

## 4. Repository structure

```
/
├── BUILD_PROMPT.md                  (this file)
├── README.md
├── ACCEPTANCE_CRITERIA_CHECKLIST.md
├── docker-compose.yml
├── nginx.conf
├── .env.example
├── .gitignore                       (includes .env, target/, *.log)
├── .agent/rules/project-rules.md
├── .agent/workflows/verify-phase.md
├── database/
│   ├── 01-schema.sql
│   └── 02-data.sql
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/edu/sist/prs/
│       ├── PrsApplication.java
│       ├── config/        (SecurityConfig, CorsConfig (dev profile only), SchedulerConfig, JacksonConfig if needed)
│       ├── controller/
│       ├── service/
│       ├── engine/        (pure calculation classes with no Spring or JPA dependencies: CapacityCalculator, ConflictRules, SkillMatcher, BudgetCalculator, DeliveryForecaster)
│       ├── repository/
│       ├── entity/
│       ├── dto/           (request/ and response/ sub-packages)
│       ├── exception/     (custom exceptions + GlobalExceptionHandler)
│       └── util/          (DateRanges, WorkingDays)
│   └── src/test/java/edu/sist/prs/   (mirrors main)
├── frontend/
│   ├── login.html, pm-dashboard.html, dev-dashboard.html, projects.html, project-details.html,
│   │   milestones.html, resources.html, allocation-matrix.html, timeline.html, heatmap.html,
│   │   conflicts.html, budget.html, leave.html, simulator.html, notifications.html
│   ├── css/  (base.css, layout.css, components.css)
│   └── js/   (api.js, auth.js, ui.js, one file per page)
├── postman/PRS.postman_collection.json
└── docs/
    ├── PROGRESS.md
    ├── DECISIONS.md
    ├── ARCHITECTURE.md
    ├── ER_DIAGRAM.md          (text specification of the ER diagram)
    ├── architecture.drawio
    └── er-diagram.drawio
```

**Engine design (important).** Put every formula from Section 9 in plain Java classes in `engine/` that take plain data (records or lists) and return results. They must not call repositories. Services load data from repositories, call the engine, and save results. This is what lets the what-if simulator (Section 9.8) run the same code on an in-memory copy, and it makes unit tests simple.

---

## 5. Global business rules

| ID | Rule |
| --- | --- |
| R1 Dates | All date ranges are inclusive of start and end. Ranges A and B overlap when `A.start <= B.end && B.start <= A.end`. Implement once in `util/DateRanges.overlaps(...)`. Example: 1–10 Jan and 10–20 Jan overlap on 10 Jan; 1–10 Jan and 11–20 Jan do not overlap. |
| R2 Block vs flag | A single allocation must have 1 ≤ `allocation_pct` ≤ 100, otherwise 400. If saving (create or update) would make the resource's load exceed its capacity on any day, and `confirmOverride` is not `true`, return 409 with error code `RESOURCE_OVERALLOCATION` and details (dates, peak load). If `confirmOverride=true`, save with `override_confirmed = true`, then the conflict engine records the conflict. Nothing over capacity is ever saved silently. |
| R3 Capacity | `capacity(r, d) = 0` if r is INACTIVE, or d is inside an APPROVED `unavailability` of r, or (for DEVELOPER only) d is Saturday or Sunday. Otherwise `capacity(r, d) = r.capacity_pct`. HARDWARE and CLOUD use calendar days (no weekend rule). |
| R4 Over-allocation | Evaluated only on "available days": days where r is ACTIVE, not inside APPROVED unavailability, and (for DEVELOPER) not a weekend. On an available day, over-allocated when `load(r, d) > capacity(r, d)`: CRITICAL if `load > 100`; WARNING if `capacity < load <= 100`. `load == capacity == 100` is NOT a conflict. Load on leave/maintenance days is reported ONLY as LEAVE_CONFLICT; load on an INACTIVE resource ONLY as INACTIVE_RESOURCE; weekends are ignored for developers. Each problem is reported once. |
| R5 Working time | One developer working day = 8 hours. Weekends are not working days. Public holidays are not modelled (documented limitation). |
| R6 Skills | Assigning a developer whose skill match (Section 9.3) for the milestone is below `skill.mismatch.pct` (50) requires `confirmOverride=true`, otherwise 409 `SKILL_MISMATCH`. A mandatory milestone skill that no ACTIVE developer holds at the required level raises SKILL_GAP. Never auto-assign. |
| R7 Simulation | Simulation reads real data inside a read-only transaction, applies changes to an in-memory copy, and never writes to `allocation`. It saves a `simulation_scenario` only when the PM calls the save endpoint. |
| R8 Money | Currency INR (₹). Stored as DECIMAL(12,2) (DECIMAL(14,2) for project budgets). Java `BigDecimal`. |
| R9 Completed work | Allocations whose milestone status is COMPLETED, or whose project status is COMPLETED or ARCHIVED, are ignored by load, conflicts and forecasts. |
| R10 Deletion | Deleting an allocation re-runs conflict detection for that resource and project; conflicts no longer found are set RESOLVED. Projects are never hard-deleted: DELETE sets status ARCHIVED. |
| R11 Thresholds | All thresholds are rows in `settings` (Section 8), read through one `SettingsService` with an in-memory cache refreshed when an ADMIN updates settings. |

---

## 6. Resource types

All resources share the `resource` table; type-specific fields live in detail tables.

| Type | Examples | Capacity meaning | Cost basis | Main conflict |
| --- | --- | --- | --- | --- |
| DEVELOPER | Arun, Priya | % of working time (0–100) | `cost_rate` per HOUR | load above capacity on any working day |
| HARDWARE | GPU workstation, IoT test kit | % of the device's time on a calendar day | `cost_rate` per DAY | two bookings adding above 100% on the same day |
| CLOUD | AWS EC2 pool | % of `daily_quota_units` | `cost_rate` per UNIT (e.g. per vCPU-hour) | planned units above quota on any day (CLOUD_QUOTA), or planned month cost above `monthly_cap` |

For CLOUD: planned units on day d = `load(r, d) / 100 × daily_quota_units`. Over-quota days are reported as CLOUD_QUOTA (not OVER_ALLOCATION).

A developer profile shows: name, email, job title, skills with proficiency (1 beginner, 2 intermediate, 3 expert), hourly cost, capacity %, status, leave, and CALCULATED values: current load (today), peak load (next 30 days), remaining capacity (today), list of active projects with their %.

---

## 7. Database schema (MySQL 8.0, InnoDB, utf8mb4)

18 tables. Every table except link tables and `settings` has `id BIGINT AUTO_INCREMENT PRIMARY KEY`, `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`.

Enumerations are `VARCHAR(20)` with a `CHECK (col IN (...))` constraint and mapped with `@Enumerated(EnumType.STRING)`. Do not use the MySQL `ENUM` type.

Calculated values (load, remaining capacity, spend totals, burn rate, forecasts, scores) are NEVER stored as columns.

| Table | Columns (type, constraint) | Keys and indexes |
| --- | --- | --- |
| `users` | name VARCHAR(100) NOT NULL; email VARCHAR(150) NOT NULL; password_hash VARCHAR(100) NOT NULL; role VARCHAR(20) NOT NULL CHECK IN ('PM','DEVELOPER','ADMIN'); resource_id BIGINT NULL; active BOOLEAN NOT NULL DEFAULT TRUE | UNIQUE(email); UNIQUE(resource_id); FK resource_id → resource(id) |
| `resource` | type VARCHAR(20) NOT NULL CHECK IN ('DEVELOPER','HARDWARE','CLOUD'); name VARCHAR(100) NOT NULL; status VARCHAR(20) NOT NULL CHECK IN ('ACTIVE','INACTIVE'); capacity_pct INT NOT NULL CHECK 0–100; cost_rate DECIMAL(12,2) NOT NULL CHECK ≥ 0; cost_unit VARCHAR(10) NOT NULL CHECK IN ('HOUR','DAY','UNIT') | INDEX(type, status) |
| `developer_profile` | resource_id BIGINT PK; email VARCHAR(150) NOT NULL; job_title VARCHAR(100) | FK → resource ON DELETE CASCADE; UNIQUE(email) |
| `hardware_asset` | resource_id BIGINT PK; asset_tag VARCHAR(50) NOT NULL; lab_location VARCHAR(100) | FK → resource ON DELETE CASCADE; UNIQUE(asset_tag) |
| `cloud_resource` | resource_id BIGINT PK; provider VARCHAR(50) NOT NULL; unit_name VARCHAR(30) NOT NULL; daily_quota_units DECIMAL(12,2) NOT NULL CHECK > 0; monthly_cap DECIMAL(12,2) NOT NULL CHECK > 0 | FK → resource ON DELETE CASCADE |
| `skill` | name VARCHAR(60) NOT NULL; category VARCHAR(60) | UNIQUE(name) |
| `resource_skill` | resource_id BIGINT; skill_id BIGINT; proficiency INT NOT NULL CHECK 1–3 | PK(resource_id, skill_id); FKs; only DEVELOPER resources (service check) |
| `project` | code VARCHAR(20) NOT NULL; name VARCHAR(150) NOT NULL; description TEXT; pm_user_id BIGINT NOT NULL; start_date DATE NOT NULL; end_date DATE NOT NULL; status VARCHAR(20) NOT NULL CHECK IN ('PLANNED','ACTIVE','ON_HOLD','COMPLETED','ARCHIVED'); total_budget DECIMAL(14,2) NOT NULL CHECK > 0 | UNIQUE(code); FK pm_user_id → users; CHECK(end_date >= start_date) |
| `milestone` | project_id BIGINT NOT NULL; name VARCHAR(150) NOT NULL; description TEXT; start_date DATE NOT NULL; end_date DATE NOT NULL; status VARCHAR(20) NOT NULL CHECK IN ('PLANNED','IN_PROGRESS','COMPLETED','DELAYED','AT_RISK'); progress_pct INT NOT NULL DEFAULT 0 CHECK 0–100; estimated_effort_hours DECIMAL(10,2) NOT NULL CHECK > 0 | FK → project ON DELETE CASCADE; CHECK(end_date >= start_date); INDEX(project_id) |
| `milestone_skill_requirement` | milestone_id BIGINT; skill_id BIGINT; min_proficiency INT NOT NULL CHECK 1–3; mandatory BOOLEAN NOT NULL DEFAULT TRUE | PK(milestone_id, skill_id); FKs (milestone ON DELETE CASCADE) |
| `allocation` | resource_id BIGINT NOT NULL; milestone_id BIGINT NOT NULL; start_date DATE NOT NULL; end_date DATE NOT NULL; allocation_pct INT NOT NULL CHECK 1–100; override_confirmed BOOLEAN NOT NULL DEFAULT FALSE | FKs (milestone ON DELETE CASCADE); CHECK(end_date >= start_date); UNIQUE(resource_id, milestone_id, start_date); INDEX(resource_id, start_date, end_date); INDEX(milestone_id) |
| `unavailability` | resource_id BIGINT NOT NULL; start_date DATE NOT NULL; end_date DATE NOT NULL; type VARCHAR(20) NOT NULL CHECK IN ('LEAVE','MAINTENANCE'); status VARCHAR(20) NOT NULL CHECK IN ('REQUESTED','APPROVED','REJECTED'); reason VARCHAR(255); requested_by BIGINT NOT NULL; decided_by BIGINT NULL | FKs (resource, users); CHECK(end_date >= start_date); INDEX(resource_id, start_date, end_date) |
| `budget_transaction` | project_id BIGINT NOT NULL; txn_date DATE NOT NULL; amount DECIMAL(12,2) NOT NULL CHECK > 0; category VARCHAR(20) NOT NULL CHECK IN ('LABOUR','HARDWARE','CLOUD','OTHER'); resource_id BIGINT NULL; note VARCHAR(255) | FKs; INDEX(project_id, txn_date) |
| `conflict` | type VARCHAR(30) NOT NULL; severity VARCHAR(10) NOT NULL CHECK IN ('INFO','WARNING','CRITICAL'); project_id BIGINT NULL; milestone_id BIGINT NULL; resource_id BIGINT NULL; start_date DATE NULL; end_date DATE NULL; peak_value DECIMAL(10,2) NULL; description VARCHAR(500) NOT NULL; status VARCHAR(20) NOT NULL CHECK IN ('OPEN','ACKNOWLEDGED','RESOLVED'); detected_at TIMESTAMP NOT NULL; resolved_at TIMESTAMP NULL; dedup_key VARCHAR(200) NOT NULL | UNIQUE(dedup_key); INDEX(status, severity); INDEX(project_id); INDEX(resource_id); FKs ON DELETE SET NULL |
| `simulation_scenario` | project_id BIGINT NOT NULL; name VARCHAR(150) NOT NULL; created_by BIGINT NOT NULL; baseline_json JSON NOT NULL; result_json JSON NOT NULL | FKs; INDEX(project_id) |
| `simulation_change` | scenario_id BIGINT NOT NULL; change_type VARCHAR(20) NOT NULL CHECK IN ('SWAP','ADD','REMOVE','CHANGE_PCT'); allocation_id BIGINT NULL; from_resource_id BIGINT NULL; to_resource_id BIGINT NULL; milestone_id BIGINT NULL; start_date DATE NULL; end_date DATE NULL; new_pct INT NULL CHECK 1–100 | FK scenario ON DELETE CASCADE; allocation_id is NOT a foreign key (the allocation may be deleted later) |
| `notification` | user_id BIGINT NOT NULL; project_id BIGINT NULL; conflict_id BIGINT NULL; severity VARCHAR(10) NOT NULL; title VARCHAR(150) NOT NULL; message VARCHAR(500) NOT NULL; is_read BOOLEAN NOT NULL DEFAULT FALSE | FK user; FKs project/conflict ON DELETE SET NULL; INDEX(user_id, is_read) |
| `settings` | setting_key VARCHAR(100) PRIMARY KEY; setting_value VARCHAR(100) NOT NULL; description VARCHAR(255) | — |

Map JSON columns with Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` on a `String` field. Use `spring.jpa.hibernate.ddl-auto=validate`. If validation reports a type mismatch, fix the entity mapping (for example `columnDefinition`); never switch to `update` or `create`.

Required validation outside SQL (service layer): allocation dates inside milestone dates; milestone dates inside project dates; `resource_skill` only for DEVELOPER; `users.resource_id` only for role DEVELOPER and only to a DEVELOPER resource; no allocation to an INACTIVE resource (400 `RESOURCE_INACTIVE`); duplicate allocation → 409 `DUPLICATE_ALLOCATION`.

---

## 8. Settings (seeded rows)

| Key | Value | Meaning |
| --- | --- | --- |
| `burnout.threshold.pct` | 85 | Weekly average load that counts toward burnout |
| `burnout.consecutive.weeks` | 3 | Consecutive weeks needed for BURNOUT_RISK |
| `burn.window.days` | 14 | Days used for burn rate |
| `budget.cpi.warning` | 0.9 | CPI below this raises BUDGET_RISK WARNING |
| `budget.critical.days` | 14 | Run-out within this many days of today and before project end → CRITICAL |
| `deadline.warning.days` | 7 | Days before milestone end for a deadline notice |
| `deadline.progress.pct` | 80 | Progress below this triggers the deadline notice |
| `delivery.warning.days` | 5 | Delay up to this many working days is WARNING, above is CRITICAL |
| `skill.mismatch.pct` | 50 | Skill match below this is SKILL_MISMATCH |
| `unstaffed.lookahead.days` | 14 | Milestones starting within this window must have a developer |
| `ranking.weight.skill` | 0.5 | Candidate ranking weight |
| `ranking.weight.fit` | 0.3 | Candidate ranking weight |
| `ranking.weight.cost` | 0.2 | Candidate ranking weight |
| `hours.per.day` | 8 | Working hours per developer day |
| `forecast.max.days` | 1825 | Safety cap on day-by-day forecasting loops |

The three ranking weights must sum to 1.0 (ADMIN update rejected otherwise, 400).

---

## 9. Calculations and engines

Every formula below has a worked example. The worked examples are REQUIRED unit tests (Section 16) with exactly these numbers.

### 9.1 Capacity and load (all resource types)

```
capacity(r, d) = per Rule R3
load(r, d)     = Σ allocation_pct of allocations a of r where a.start ≤ d ≤ a.end, excluding Rule R9 allocations
remaining(r, d) = capacity(r, d) − load(r, d)      (may be negative)
```

- Profile values: current load = load(r, today); peak load = max load over [today, today+29]; remaining = remaining(r, today).
- Implementation: one query loads all allocations and all APPROVED unavailability of the resource that overlap the window; the engine walks the days in memory.
- Over-allocated available days are merged into one period when no available, non-over-allocated day lies between them (weekends and leave days do not break a period, but they are not counted in it). Period start = first over-allocated day, end = last, `peak_value` = max load in the period.

Worked example 1 (AC2): Arun, capacity 100%. Milestone A 1–20 Jan at 60%; Milestone B 10–25 Jan at 50%. Expected load: 60 on 1–9 Jan, 110 on 10–20 Jan, 50 on 21–25 Jan; one OVER_ALLOCATION with peak 110, CRITICAL. Because Arun is a DEVELOPER, weekend days have capacity 0 and are not reported as over-allocation days, but the merged period still spans the overlapping working days (see Clarification 6). Write two tests: one with a HARDWARE resource on calendar days (period exactly 10–20 Jan), and one with a DEVELOPER, asserting only on weekdays.

Worked example 2: Priya capacity 100%, 50% + 30% on the same dates → load 80, remaining 20, no conflict.

Worked example (AC1): Arun 40% on Project A and 35% on Project B on the same dates → load 75, remaining 25.

### 9.2 Conflict detection engine

| Type | Detected when | Severity | dedup_key pattern |
| --- | --- | --- | --- |
| OVER_ALLOCATION | DEVELOPER or HARDWARE: load > capacity on any available day (Rule R4; merged periods) | CRITICAL if peak > 100, else WARNING | `OVERALLOC:{resourceId}:{start}:{end}` |
| LEAVE_CONFLICT | load > 0 on a day inside APPROVED unavailability (LEAVE or MAINTENANCE); one conflict per affected allocation | CRITICAL | `LEAVE:{allocationId}:{unavailabilityId}` |
| INACTIVE_RESOURCE | an allocation (not excluded by R9) points to an INACTIVE resource | CRITICAL | `INACTIVE:{allocationId}` |
| SKILL_GAP | a mandatory milestone skill is held by no ACTIVE developer at ≥ min_proficiency (CRITICAL); or held but every holder has remaining < 1% on every working day of the milestone (WARNING, "skill exists but fully booked") | CRITICAL / WARNING | `SKILLGAP:{milestoneId}:{skillId}` |
| SKILL_MISMATCH | an allocated developer's skill match for the milestone < `skill.mismatch.pct` | WARNING | `MISMATCH:{allocationId}` |
| UNSTAFFED_MILESTONE | PLANNED or IN_PROGRESS milestone with start ≤ today + `unstaffed.lookahead.days` and end ≥ today, and no DEVELOPER allocation | WARNING | `UNSTAFFED:{milestoneId}` |
| BURNOUT_RISK | Section 9.4 | WARNING | `BURNOUT:{resourceId}:{firstWeekMonday}` |
| CLOUD_QUOTA | CLOUD: load > 100% of quota on any day (merged periods), or planned cost in a calendar month > `monthly_cap` | CRITICAL | `CLOUDQ:{resourceId}:{start}:{end}` / `CLOUDCAP:{resourceId}:{yyyy-MM}` |
| BUDGET_RISK | Section 9.6 | WARNING / CRITICAL | `BUDGET:{projectId}:{reason}` (reason ∈ PLAN_EXCEEDS, EXCEEDED, EARLY_RUNOUT, LOW_CPI) |
| MILESTONE_DELIVERY_RISK | forecast finish (9.7) > milestone end_date, for PLANNED or IN_PROGRESS milestones | WARNING if delay ≤ `delivery.warning.days` working days, else CRITICAL; CRITICAL "cannot forecast" only when developers are allocated but give zero hours (see 9.7) | `DELIVERY:{milestoneId}` |

**When it runs.**

1. Synchronously, at the end of every transaction that changes allocation, unavailability (approve/reject/create maintenance), resource status/capacity, milestone (dates, status, progress, effort, skills), resource skills, or budget transactions: re-evaluate only the affected resources and projects.
2. Nightly at 01:00 server time (`@Scheduled(cron = "0 0 1 * * *")`): full scan of all ACTIVE projects and resources.
3. On demand: `POST /api/conflicts/scan` (PM, ADMIN).

**Scan window.** From today − 28 days to the latest active milestone end date (at least today + 90 days).

**Persistence (upsert).** For each detected conflict: if `dedup_key` exists → update description, severity, peak_value, dates, and set status OPEN if it was RESOLVED (keep ACKNOWLEDGED as is); else insert with status OPEN and create notifications (Section 12). For each OPEN or ACKNOWLEDGED conflict within the re-evaluated scope whose key was NOT detected → set RESOLVED, `resolved_at = now`, and create one INFO notification. Wrap in one transaction.

**Descriptions** are generated from data, e.g. `"Arun Kumar is allocated at 110% between 10 Jan 2027 and 20 Jan 2027 (capacity 100%)."`

### 9.3 Skill matching, skill gaps and candidate ranking

For milestone m with requirement set S (all rows in `milestone_skill_requirement`) and developer r:

```
credit(r, s) = 0                                   if r lacks s
             = min(1, proficiency(r, s) / min_proficiency(s))   otherwise
match(r, m)  = (Σ_s credit(r, s) / |S|) × 100      ;  match = 100 when S is empty
```

Worked example: m needs Java, Spring Boot, MySQL (each min 2). Arun has Java 3, Spring Boot 2, Python 2 → credits 1, 1, 0 → match = 66.67% (display 67%), missing = [MySQL].

**Candidate ranking** — `GET /api/milestones/{id}/candidates?pct=P` (1 ≤ P ≤ 100):

- Candidates: ACTIVE DEVELOPER resources, excluding those with credit 0 on every mandatory skill (when at least one mandatory skill exists).
- `skill = match / 100`.
- `minRemaining` = minimum of remaining(r, d) over working days d of the milestone where capacity(r, d) > 0; if there are no such days, fit = 0.
- `fit = clamp(minRemaining / P, 0, 1)`.
- `costNorm = (rate − minRate) / (maxRate − minRate)` across the candidate list; 0 when maxRate = minRate. `cheapness = 1 − costNorm`.
- `score = w_skill × skill + w_fit × fit + w_cost × cheapness` (weights from settings).
- Flag `wouldOverload = true` when minRemaining < P.
- Sort by score descending, then by name. Response includes each part (skill, fit, cheapness, score), missing skills, current peak load, and wouldOverload.

Worked example: P = 60. Karthik: match 100, 60% free, ₹650 (max). Arun: match 66.67, 30% free, ₹500 (min). Karthik = 0.5×1 + 0.3×1 + 0.2×0 = 0.800. Arun = 0.5×0.6667 + 0.3×0.5 + 0.2×1 = 0.683. Karthik ranks first.

### 9.4 Burnout (sustained load)

- Weeks are ISO weeks (Monday–Sunday).
- Weekly load(r, w) = average of load(r, d) over working days d in w where capacity(r, d) > 0. Weeks with no such days break a streak.
- If weekly load ≥ `burnout.threshold.pct` for ≥ `burnout.consecutive.weeks` consecutive weeks within the scan window → BURNOUT_RISK (one conflict per streak, keyed by the streak's first Monday).
- DEVELOPER resources only.

Worked example: weekly loads 90, 95, 88 → BURNOUT_RISK. Weekly loads 90, 95, 80 → none.

### 9.5 Leave and availability

1. `POST /api/unavailability` with type LEAVE: a DEVELOPER may request for themselves only (status REQUESTED); a PM may create for any developer (status REQUESTED or directly APPROVED if `approve=true`).
2. `PUT /api/unavailability/{id}/approve` or `/reject` (PM). Only APPROVED rows affect capacity (Rule R3).
3. MAINTENANCE (HARDWARE only) is created by PM or ADMIN directly as APPROVED.
4. After approval, the conflict engine runs for that resource: every allocation with load on a leave day gets a LEAVE_CONFLICT (so a leave overlapping two projects creates two conflicts).
5. Leave days contribute 0 hours in the delivery forecast (9.7).

Validation: end ≥ start (400 `INVALID_DATE_RANGE`); overlapping APPROVED or REQUESTED leave for the same resource → 409 `DUPLICATE_LEAVE`.

Worked example: Rahul capacity 100%, allocated 80% on a milestone covering 10–14 Sep; approved leave 10–14 Sep (weekdays) → CRITICAL LEAVE_CONFLICT for those dates.

### 9.6 Cost, budget, burn rate, forecast and Earned Value

**Planned cost of an allocation a on resource r:**

- DEVELOPER: Σ over days d in a where d is a working day and capacity(r, d) > 0 of `a.pct/100 × hours.per.day × cost_rate`.
- HARDWARE: Σ over calendar days d in a with capacity(r, d) > 0 of `a.pct/100 × cost_rate`.
- CLOUD: Σ over calendar days d in a with capacity(r, d) > 0 of `a.pct/100 × daily_quota_units × cost_rate`.

Milestone planned cost = Σ of its allocations. Project planned cost (plan total) = Σ of its milestones.

**Actual spend** = Σ `budget_transaction.amount` with txn_date ≤ today, also returned per category.

```
remaining    = total_budget − actualSpend
utilisation% = actualSpend / total_budget × 100
W            = burn.window.days, or (today − first txn_date + 1) if that is smaller (minimum 1)
burnRate     = (Σ amount with txn_date in [today − W + 1, today]) / W
daysLeft     = remaining / burnRate
runOutDate   = today + ceil(daysLeft) days
```

Rules:

- No transactions, or burnRate = 0 → `forecastStatus = "INSUFFICIENT_DATA"`, message "Insufficient spending data", no date.
- remaining = 0 → "Budget exhausted". remaining < 0 → "Budget exceeded by ₹X" and BUDGET_RISK EXCEEDED (CRITICAL).
- Plan total > total_budget → BUDGET_RISK PLAN_EXCEEDS (CRITICAL).
- runOutDate < project end_date → BUDGET_RISK EARLY_RUNOUT: CRITICAL if runOutDate ≤ today + `budget.critical.days`, else WARNING.
- Message: `"At the current burn rate, {project} is projected to exhaust its budget around {runOutDate formatted d MMM yyyy}."`

**Earned Value:**

```
PV  = Σ over allocations of planned cost for days ≤ today
EV  = Σ over milestones of (progress_pct / 100 × milestone planned cost)
CPI = EV / actualSpend        (null when actualSpend = 0)
SPI = EV / PV                 (null when PV = 0)
EAC = total_budget / CPI      (null when CPI is null or 0)
```

CPI < `budget.cpi.warning` → BUDGET_RISK LOW_CPI (WARNING).

Worked example 5 (brief): total ₹500,000; actual ₹300,000; spend in last 14 days ₹140,000 → remaining ₹200,000, utilisation 60%, burn rate ₹10,000/day, daysLeft 20, runOutDate = today + 20.

Worked EVM example: PV ₹320,000; EV ₹280,000; actual ₹300,000 → CPI 0.9333, SPI 0.875, EAC ₹535,714.29; no LOW_CPI warning (0.9333 ≥ 0.9).

**Forecast endpoint response** `GET /api/projects/{id}/budget/forecast`: totalBudget, actualSpend, spendByCategory, remaining, utilisationPct, burnRate, windowDays, daysLeft, runOutDate, forecastStatus, message, planTotal, pv, ev, cpi, spi, eac, and `series` for the chart: daily points from project start to max(project end, runOutDate) with `actualCumulative` (up to today), `projectedCumulative` (from today at burnRate), `plannedValueCumulative`, and the constant `budget`. Also `remainingSeries` for the burn-down chart.

### 9.7 Delivery date forecast

```
remainingEffort(m) = estimated_effort_hours × (1 − progress_pct / 100)
eff(r, m)          = 0.5 + 0.5 × match(r, m) / 100
hours(m, d)        = Σ over DEVELOPER allocations a of m:  a.pct/100 × hours.per.day × eff(r_a, m)
                     counting only days where d is a working day and capacity(r_a, d) > 0
```

- Start day = max(today, milestone start). Walk forward day by day, accumulating hours(m, d). Forecast finish = the first day the total ≥ remainingEffort.
- If an allocation ends before completion, assume it continues at the same percentage (set `extended = true` in the response).
- If remainingEffort = 0 → forecast finish = today (or the day progress reached 100).
- If there are no developer allocations, return `forecastStatus = "NO_CAPACITY"` with message "Cannot forecast: no working capacity"; do NOT raise MILESTONE_DELIVERY_RISK in this case (UNSTAFFED_MILESTONE already covers it).
- If developers are allocated but no hours accumulate within `forecast.max.days` (for example everyone on long leave), return `NO_CAPACITY` and raise MILESTONE_DELIVERY_RISK (CRITICAL).
- delayWorkingDays = working days strictly after end_date up to and including forecast finish (0 if on time).
- Project forecast delivery = latest milestone forecast finish.
- HARDWARE and CLOUD do not change speed.
- The speed factor `eff` is a documented modelling assumption, not measured.

Worked example: 320 h estimated, 25% done → 240 h left. Arun 60%, match 66.67 → eff 0.8333 → 0.6 × 8 × 0.8333 = 4.0 h/day. Karthik 50%, match 100 → 4.0 h/day. Team 8.0 h/day → 30 working days (no leave). If the deadline is 25 working days away → delay 5 → WARNING.

### 9.8 What-if simulator

Request `POST /api/simulations/run`:

```json
{
  "projectId": 1,
  "changes": [
    { "type": "SWAP", "allocationId": 12, "toResourceId": 5 },
    { "type": "CHANGE_PCT", "allocationId": 14, "newPct": 40 },
    { "type": "ADD", "resourceId": 3, "milestoneId": 7, "startDate": "2027-01-10", "endDate": "2027-01-31", "pct": 30 },
    { "type": "REMOVE", "allocationId": 15 }
  ]
}
```

Steps:

1. `@Transactional(readOnly = true)`. Load the project, its milestones and allocations, and ALL allocations and approved unavailability of every resource involved (current and new) that overlap the project's date range, plus the project's transactions.
2. Build plain in-memory snapshots (engine inputs). Copy them. Apply changes to the copy only. Validate each change (allocation belongs to the project, target resource ACTIVE and of the same type, pct 1–100, dates inside the milestone); invalid → 400 with the index of the bad change.
3. Run the SAME engine code (9.1–9.7 and the conflict rules of 9.2) on the original and on the copy.
4. Return, side by side: per changed allocation (resource, pct, dates, skill match, planned cost); per milestone (skill match of team, forecast finish, delay); project planned cost; project forecast delivery; budget run-out date and planned-cost-vs-budget; peak load per involved resource; conflicts only in current (removed); conflicts only in simulated (added); and a recommendation.
5. Recommendation (rule-based):
   - any CRITICAL conflict added → `DO_NOT_APPLY`;
   - else if no conflicts added, project delivery not later, and simulated plan total ≤ total_budget → `APPLY`;
   - else `REVIEW`, with a generated sentence listing the trade-offs (e.g. "Delivery 10 working days earlier; planned cost ₹2,400 lower; 1 new WARNING conflict").
6. Nothing is written. `POST /api/simulations` (save) re-runs the simulation server-side, then stores the scenario, changes, baseline_json and result_json in one transaction (201). `GET /api/simulations/{id}/report.pdf` builds the PDF from the stored result_json.

Required test: count and checksum (e.g. ordered list of id, resource_id, pct, dates) of `allocation` rows are identical before and after `/run` and `/api/simulations` save.

Worked example 6 (swap): milestone needs Java (2) and Spring Boot (2); 144 h left; allocation at 60%.

| | Developer A (current) | Developer B (simulated) |
| --- | --- | --- |
| Skills | Java 2 | Java 2, Spring Boot 2 |
| Rate | ₹500/h | ₹650/h |
| Match / eff | 50% / 0.75 | 100% / 1.0 |
| Hours per day | 3.6 | 4.8 |
| Working days to finish | 40 | 30 |
| Cost to finish (60% × 8 h × rate × days) | ₹96,000 | ₹93,600 |

If B already has 50% elsewhere on overlapping days → B peaks at 110% → CRITICAL added → `DO_NOT_APPLY`. If instead the swap moves A's allocation off an over-allocated period and B stays ≤ 100% → A's OVER_ALLOCATION appears under "removed".

### 9.9 Project health (dashboard badge)

- RED: at least one OPEN or ACKNOWLEDGED CRITICAL conflict linked to the project (directly or through its milestones or allocations).
- AMBER: no CRITICAL, at least one WARNING.
- GREEN: none.

"At-risk projects" on the PM dashboard = projects with health RED or AMBER.

---

## 10. REST API

### 10.1 Response envelope

Success:

```json
{ "status": "SUCCESS", "data": {}, "message": "Allocation created", "timestamp": "2026-09-18T10:30:00" }
```

Error:

```json
{ "status": "ERROR", "errorCode": "RESOURCE_OVERALLOCATION", "message": "Arun Kumar would be at 110% from 10 Jan 2027 to 20 Jan 2027", "details": {}, "fieldErrors": [ { "field": "allocationPct", "message": "must be between 1 and 100" } ], "timestamp": "2026-09-18T10:30:00" }
```

Dates in JSON are ISO strings (`2027-01-10`); timestamps ISO local date-time. Configure `spring.jackson.serialization.write-dates-as-timestamps=false`.

### 10.2 Status codes and error codes

| HTTP | When | errorCode examples |
| --- | --- | --- |
| 200 | Read or update OK | — |
| 201 | Created | — |
| 204 | Deleted (no body) | — |
| 400 | Invalid input or business validation | `VALIDATION_FAILED`, `INVALID_DATE_RANGE`, `RESOURCE_INACTIVE`, `INVALID_SETTINGS` |
| 401 | Not logged in | `UNAUTHENTICATED` |
| 403 | Wrong role or not own data | `FORBIDDEN` |
| 404 | Not found | `NOT_FOUND` |
| 409 | Business conflict | `RESOURCE_OVERALLOCATION`, `SKILL_MISMATCH`, `DUPLICATE_ALLOCATION`, `DUPLICATE_LEAVE`, `DUPLICATE_EMAIL` |
| 500 | Unexpected error (message generic, details only in logs) | `INTERNAL_ERROR` |

One `@RestControllerAdvice` produces every error response. 401 and 403 from Spring Security must use the same JSON envelope (custom `AuthenticationEntryPoint` and `AccessDeniedHandler`).

### 10.3 Endpoints

| Area | Method and path | Roles |
| --- | --- | --- |
| Auth | `POST /api/auth/login` {email, password} → user {id, name, role, resourceId}; `POST /api/auth/logout`; `GET /api/auth/me` | public / any / any |
| Projects | `GET /api/projects?status=&q=&page=&size=`; `POST /api/projects`; `GET /api/projects/{id}`; `PUT /api/projects/{id}`; `DELETE /api/projects/{id}` (archives) | PM |
| Milestones | `GET /api/projects/{id}/milestones`; `POST /api/projects/{id}/milestones`; `PUT /api/milestones/{id}`; `DELETE /api/milestones/{id}`; `PATCH /api/milestones/{id}/progress` {progressPct} | PM; DEVELOPER may PATCH progress on milestones they are allocated to |
| Skill requirements | `PUT /api/milestones/{id}/skills` [{skillId, minProficiency, mandatory}] | PM |
| Candidates | `GET /api/milestones/{id}/candidates?pct=60` | PM |
| Milestone forecast | `GET /api/milestones/{id}/forecast` | PM |
| Resources | `GET /api/resources?type=&status=&q=&page=&size=`; `POST /api/resources`; `GET /api/resources/{id}` (profile with calculated values); `PUT /api/resources/{id}`; `DELETE /api/resources/{id}` (sets INACTIVE); `GET /api/resources/{id}/capacity?from=&to=` (daily capacity, load, remaining) | PM, ADMIN (read also PM) |
| Resource skills | `PUT /api/resources/{id}/skills` [{skillId, proficiency}] | PM, ADMIN |
| Skills | `GET /api/skills`; `POST /api/skills` | any logged-in / ADMIN, PM |
| Allocations | `GET /api/allocations?projectId=&resourceId=&milestoneId=`; `POST /api/allocations` {resourceId, milestoneId, startDate, endDate, allocationPct, confirmOverride}; `PUT /api/allocations/{id}` (same body); `DELETE /api/allocations/{id}` | PM |
| Allocation matrix | `GET /api/projects/{id}/matrix` (resources × milestones with pct, row totals, peak loads) | PM |
| Unavailability | `GET /api/unavailability?resourceId=&from=&to=&status=`; `POST /api/unavailability`; `PUT /api/unavailability/{id}/approve`; `PUT /api/unavailability/{id}/reject`; `DELETE /api/unavailability/{id}` (only REQUESTED, by requester or PM) | see 9.5 |
| Budget | `GET /api/projects/{id}/transactions`; `POST /api/projects/{id}/transactions`; `POST /api/projects/{id}/transactions/import` (CSV: date,amount,category,note); `GET /api/projects/{id}/budget`; `GET /api/projects/{id}/budget/forecast` | PM |
| Conflicts | `GET /api/conflicts?severity=&type=&status=&projectId=&resourceId=&page=&size=`; `GET /api/conflicts/project/{projectId}`; `PUT /api/conflicts/{id}/acknowledge`; `POST /api/conflicts/scan` | PM (ADMIN read, scan) |
| Timeline | `GET /api/projects/{id}/timeline` (milestones with dates, status, progress, forecast, resources); `GET /api/timeline/resources?from=&to=&type=DEVELOPER` (lanes per resource with allocation bars and conflict bands) | PM |
| Heatmap | `GET /api/heatmap?from=&to=&type=DEVELOPER&granularity=DAY|WEEK` → rows {resource, cells[{date, loadPct, capacityPct, band}]} with band LOW (<50), HEALTHY (50–85), HIGH (>85–100), OVER (>100 or > capacity) | PM |
| Simulation | `POST /api/simulations/run`; `POST /api/simulations` {name, projectId, changes}; `GET /api/simulations?projectId=`; `GET /api/simulations/{id}`; `GET /api/simulations/{id}/report.pdf` | PM |
| Dashboards | `GET /api/dashboard/pm`; `GET /api/dashboard/developer/me` | PM / DEVELOPER |
| Notifications | `GET /api/notifications?unread=true&page=&size=`; `GET /api/notifications/unread-count`; `PUT /api/notifications/{id}/read`; `PUT /api/notifications/read-all` | any, own only |
| Admin | `GET /api/admin/users`; `POST /api/admin/users`; `PUT /api/admin/users/{id}`; `PUT /api/admin/users/{id}/password`; `GET /api/admin/settings`; `PUT /api/admin/settings` | ADMIN |
| Health | `GET /api/actuator/health` | public |

**PM dashboard payload:** totals (projects, active, at-risk), openConflicts by severity, skillGaps count, average utilisation (mean current load of ACTIVE developers), per-project rows (name, health, progress = effort-weighted average of milestone progress, utilisation of budget, run-out date), upcoming deadlines (next 14 days), latest 5 notifications.

**Developer dashboard payload:** my current load, peak load (30 days), remaining, my allocations (project, milestone, pct, dates), my milestones with deadlines, weekly load for the next 8 weeks, my leave, open conflicts about me, unread notifications.

### 10.4 Validation (DTOs)

Use `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Positive`, `@PositiveOrZero`, `@Min`, `@Max`, `@DecimalMin` as appropriate. Use Java records for DTOs. Validate date order and business rules in services. Every multi-row write is `@Transactional`.

---

## 11. Security

- Spring Security 6, `SecurityFilterChain` bean, `@EnableMethodSecurity`, `@PreAuthorize` on every controller method according to Section 2.
- Session-based login: `POST /api/auth/login` authenticates with `AuthenticationManager` and stores the `SecurityContext` in the HTTP session (explicitly save it via the security context repository — verify with a test that a second request is authenticated).
- Passwords: `BCryptPasswordEncoder`. Never log passwords.
- CSRF: keep enabled, using a cookie-based token (`XSRF-TOKEN` cookie readable by JS, sent back in the `X-XSRF-TOKEN` header by `api.js`). Verify: a POST without the header → 403; with it → success. `/api/auth/login` may be exempt.
- Session cookie: HttpOnly, SameSite=Lax.
- Public paths: `/api/auth/login`, `/api/actuator/health`. Everything else under `/api/**` requires authentication.
- Developer ownership: services check that `/me` data and progress updates belong to the logged-in developer.
- Actuator: expose only `health`; set `management.endpoints.web.base-path=/api/actuator`.
- CORS: none needed in production (same origin via Nginx). In the `dev` profile only, allow `http://localhost:5500` and `http://127.0.0.1:5500` with credentials, for serving the frontend with a local static server.
- SQL: only JPA/parameterised queries; never string-concatenated SQL.

---

## 12. Notifications, scheduler and PDF

**Notifications.** Created only when a conflict is NEW (inserted) or becomes RESOLVED (one INFO), plus nightly date-based notices:

- milestone end within `deadline.warning.days` and progress < `deadline.progress.pct` (dedup per milestone per day);
- budget run-out date crosses before the project end date.

Recipients: the project's PM (via `project.pm_user_id`) for anything linked to a project; the developer user linked to the resource for OVER_ALLOCATION, LEAVE_CONFLICT, BURNOUT_RISK and SKILL_MISMATCH about them. Title and message are generated from data. Example message: `"CRITICAL: Arun Kumar is allocated at 120% between 10 Sep 2026 and 15 Sep 2026."`

The frontend polls `GET /api/notifications/unread-count` every 30 seconds for the bell badge.

**Scheduler.** One nightly job at 01:00: full conflict scan, then date-based notices. Log start, end and counts.

**PDF report** (`ReportService`, OpenPDF), generated only from a saved scenario's `result_json`:

1. Title "Project Resource Allocation Report"; project; scenario name; generated date; author.
2. Current allocation table; simulated allocation table.
3. Resource changes.
4. Current vs simulated cost; current vs simulated delivery date (project and per milestone).
5. Conflicts removed; conflicts added.
6. Skill-match changes.
7. Budget impact (run-out date before/after; plan total vs budget).
8. Recommendation and the generated trade-off sentence.

Only headings are fixed text. Response header `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="scenario-{id}.pdf"`.

---

## 13. Frontend

**General.** Semantic HTML5; CSS Grid for page layout, the heatmap and the timeline; Flexbox for bars and toolbars; Vanilla JS modules; Chart.js 4 pinned from a CDN for line/bar charts only. Responsive from 360 px to 1920 px with no horizontal page scroll (tables scroll inside their own container). Professional, dense enterprise dashboard style: consistent spacing scale, one font family, cards, tables, status badges, filters, search, modals, confirmation dialogs. No large gradients, no decorative animation.

**Shared JS.**

- `api.js`: one `request(method, path, body)` function: prefixes `/api`, sends `credentials: 'same-origin'`, JSON headers, CSRF header; unwraps the envelope; on 401 redirects to `login.html`; throws an error object with `errorCode`, `message`, `details`, `fieldErrors`.
- `auth.js`: loads `/api/auth/me`, guards pages by role (a DEVELOPER opening a PM page is redirected to `dev-dashboard.html`), renders the header (user, role, bell with unread count, logout).
- `ui.js`: loading, empty and error states; modal; confirm dialog; toast; badge helpers; date and ₹ formatting (`Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })`).

**Pages.**

| Page | Content and behaviour | Role |
| --- | --- | --- |
| `login.html` | Email and password; on success redirect by role | all |
| `pm-dashboard.html` | KPI cards; project health table (badge RED/AMBER/GREEN); budget mini-charts; upcoming deadlines; latest notifications | PM |
| `dev-dashboard.html` | Current/peak load, remaining; my allocations; my milestones and deadlines; weekly load bar chart (8 weeks); my leave with request form; conflicts about me; progress update | DEVELOPER |
| `projects.html` | Table with search and status filter, health badge; create/edit modal; archive with confirmation | PM |
| `project-details.html` | Milestones, team, budget summary, conflicts of the project | PM |
| `milestones.html` | Dates, status, progress bar, required skills editor, assigned resources, forecast finish and delay | PM |
| `resources.html` | Tabs DEVELOPER / HARDWARE / CLOUD; skills with levels; cost; capacity; current load; remaining; create/edit modal | PM, ADMIN |
| `allocation-matrix.html` | Rows = resources, columns = milestones of the selected project, cells = %; row total and peak load coloured by band; add/edit allocation modal handling the 409 over-allocation and skill-mismatch confirmation dialogs; "Find best candidate" panel showing the ranked list with skill/fit/cheapness/score and wouldOverload | PM |
| `timeline.html` | View 1: Gantt-style bars per milestone (colour = status, inner fill = progress, forecast marker). View 2: one lane per developer with allocation bars and red bands for OVER_ALLOCATION and LEAVE_CONFLICT periods. Built with CSS Grid, date columns computed in JS from API data | PM |
| `heatmap.html` | Resources × days or weeks; four bands with colour AND the % printed in each cell; legend; date range and type filters | PM |
| `conflicts.html` | Filterable table (type, severity, project, status), link to resource/milestone, Acknowledge button, Run scan button | PM |
| `budget.html` | Project selector; numbers (actual, remaining, utilisation, burn rate, days left, run-out date or "Insufficient spending data", CPI, SPI, EAC); Chart 1 line: actual cumulative (solid), projected (dashed), PV, budget (flat); Chart 2 line: remaining budget (burn-down); transactions table; add transaction; CSV import | PM |
| `leave.html` | Month grid per resource; request, approve, reject | PM; DEVELOPER own |
| `simulator.html` | Project selector; change builder (SWAP/ADD/REMOVE/CHANGE_PCT); Run; side-by-side Current vs Simulated tables with better values green and worse red (plus ▲/▼ text so colour is not the only signal); conflicts added/removed; recommendation; Save; Export PDF (enabled after save) | PM |
| `notifications.html` | List, unread first, mark read, mark all read | all |

Every page: loading state, empty state, error state (API message), and confirmation before destructive actions. Every number on every page comes from the API. No sample data in HTML or JS.

Accessibility: labels on all inputs, keyboard-reachable controls, visible focus, text contrast at least 4.5:1, status never shown by colour alone.

---

## 14. Seed data (`database/02-data.sql`)

**Dates must be relative to the database's first start**, so the demo is always meaningful: use `CURDATE()` and `DATE_ADD/DATE_SUB` (call it T). Note in the README that re-seeding requires `docker compose down -v` (deletes data).

**Users** (password for all demo users documented in README; hash generated with `BCryptPasswordEncoder` — never invented):

- ADMIN: `admin@prs.local`
- PM: Meena Iyer, `pm@prs.local`
- DEVELOPER users for each developer below (`arun@prs.local`, `priya@prs.local`, `rahul@prs.local`, `divya@prs.local`, `karthik@prs.local`), each linked to their resource.

**Skills:** Java, Spring Boot, MySQL, Python, Machine Learning, Data Analysis, React, JavaScript, UI/UX, AWS, Cloud, DevOps.

**Developers** (all ACTIVE, capacity 100, cost per HOUR):

| Name | Skills (level) | Rate |
| --- | --- | --- |
| Arun Kumar | Java 3, Spring Boot 2, MySQL 2 | ₹500 |
| Priya Sharma | Python 3, Machine Learning 3, Data Analysis 3 | ₹550 |
| Rahul Verma | React 3, JavaScript 3, UI/UX 2 | ₹450 |
| Divya Nair | AWS 2, Cloud 3, DevOps 3 | ₹600 |
| Karthik Raj | Java 3, Spring Boot 3, AWS 2 | ₹650 |

**Hardware:** GPU Workstation (asset GPU-01, AI Lab, ₹2,000/day); IoT Test Kit (asset IOT-01, Embedded Lab, ₹800/day).
**Cloud:** AWS EC2 Pool (provider AWS, unit vCPU-hour, ₹8/unit, daily quota 200 units, monthly cap ₹40,000).

**Projects** (PM = Meena):

| Code | Name | Dates | Budget | Status |
| --- | --- | --- | --- | --- |
| ALPHA | Hospital Management System | T−60 → T+60 | ₹500,000 | ACTIVE |
| BETA | AI Student Analytics | T−30 → T+75 | ₹400,000 | ACTIVE |
| GAMMA | Cloud Monitoring Platform | T−14 → T+90 | ₹600,000 | ACTIVE |

**Milestones** (choose effort hours realistically; required skills as listed):

- ALPHA M1 Backend API Development (T−30 → T+20; Java 2, Spring Boot 2, MySQL 2; IN_PROGRESS)
- ALPHA M2 Patient Portal UI (T−10 → T+30; React 2, JavaScript 2; IN_PROGRESS)
- BETA M1 Data Pipeline API (T−5 → T+25; Java 2, Spring Boot 2; IN_PROGRESS)
- BETA M2 ML Model Training (T−28 → T+30; Python 2, Machine Learning 2; IN_PROGRESS)
- GAMMA M1 Monitoring Agent (T−7 → T+40; Cloud 2, DevOps 2; IN_PROGRESS)
- GAMMA M2 Cloud Infrastructure Automation (T+10 → T+50; AWS 3 mandatory; PLANNED)
- GAMMA M3 Monitoring Dashboard (T+7 → T+60; JavaScript 2, UI/UX 2; PLANNED)

**Allocations — each creates exactly one intended demo situation:**

| # | Allocation | Intended result |
| --- | --- | --- |
| 1 | Arun → ALPHA M1, 60%, T−10 → T+10 | with #2: OVER_ALLOCATION 110% on overlapping weekdays T → T+10 (CRITICAL) |
| 2 | Arun → BETA M1, 50%, T → T+15 (override_confirmed) | swap target for the demo: moving this to Karthik removes Arun's conflict |
| 3 | Rahul → ALPHA M2, 80%, T−5 → T+25; APPROVED leave T+10 → T+14 | LEAVE_CONFLICT (CRITICAL) |
| 4 | Rahul → ALPHA M1, 10%, T → T+7 (override_confirmed) | SKILL_MISMATCH (match 0%) (WARNING); ends before Rahul's leave so it does not add a second leave conflict |
| 5 | Priya → BETA M2, 90%, T−21 → T+14 | BURNOUT_RISK (WARNING) |
| 6 | Karthik → ALPHA M1, 40%, T → T+15 | Karthik matches 66.67% (no mismatch) and has 60% free: the swap demo (#2 → Karthik, reaching 90%) stays conflict-free |
| 7 | Divya → GAMMA M1, 50%, T−7 → T+40 | normal load |
| 8 | GPU Workstation → BETA M2, 60%, T → T+10 | with #9: hardware OVER_ALLOCATION 120% (CRITICAL) |
| 9 | GPU Workstation → GAMMA M1, 60%, T+5 → T+12 | — |
| 10 | AWS EC2 Pool → GAMMA M1, 70%, T → T+10 | with #11: CLOUD_QUOTA 120% (CRITICAL) |
| 11 | AWS EC2 Pool → BETA M2, 50%, T+5 → T+10 | — |

GAMMA M2 (AWS 3 required; nobody has level 3) → SKILL_GAP (CRITICAL) and UNSTAFFED_MILESTONE (WARNING, starts T+10). GAMMA M3 (starts T+7, nobody allocated) → UNSTAFFED_MILESTONE (WARNING).

Choose `estimated_effort_hours` and `progress_pct` for each staffed milestone so that its delivery forecast is on time or at most a WARNING, and each project's plan total stays within its budget (the only intended budget risk is ALPHA EARLY_RUNOUT). Record the chosen numbers and the resulting forecasts in `docs/DECISIONS.md`.

**Budget transactions:** ALPHA: ₹160,000 spread over T−60 → T−14 (for example 8 rows × ₹20,000), plus 14 rows × ₹10,000 on T−13 … T (total ₹300,000, last-14-day spend ₹140,000) → burn rate ₹10,000/day, run-out T+20 < end T+60 → BUDGET_RISK EARLY_RUNOUT. BETA and GAMMA: modest transactions that do NOT raise budget risk. Include LABOUR, HARDWARE and CLOUD categories.

After seeding, the backend must run a full conflict scan on startup (ApplicationReadyEvent) so the Conflict Center and notifications are populated immediately. Seed SQL must not insert rows into `conflict` or `notification` directly.

Verify after Phase 3/5: each intended result above exists, and no unintended CRITICAL conflict exists. If one appears, fix the seed data, not the engine.

---

## 15. Deployment

**Containers.**

| Container | Image | Role | Port | Data |
| --- | --- | --- | --- | --- |
| `db` | `mysql:8.0` | Database; runs `database/*.sql` in alphabetical order on first start only | 3306 internal | volume `db-data` |
| `backend` | built from `backend/Dockerfile` | Spring Boot API | 8080 internal | none |
| `frontend` | `nginx:alpine` | Serves `frontend/`; proxies `/api/` to `backend:8080` | 80 public | none |

**backend/Dockerfile** (two stages):

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml:**

```yaml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: resource_allocation
      MYSQL_USER: prs_app
      MYSQL_PASSWORD: ${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql
      - ./database:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 10

  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/resource_allocation
      SPRING_DATASOURCE_USERNAME: prs_app
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
    depends_on:
      db:
        condition: service_healthy

  frontend:
    image: nginx:alpine
    volumes:
      - ./frontend:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  db-data:
```

**nginx.conf:**

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index login.html;
  location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

**Pitfalls to handle:** SQL files must be named `01-schema.sql`, `02-data.sql` (alphabetical order; they run only when the volume is empty). Hibernate `validate`, never `update`. Secrets only in `.env` (git-ignored); commit `.env.example`. Health URL is `/api/actuator/health`. Set the JVM and MySQL time zone consistently (e.g. `TZ=Asia/Kolkata` on both containers and `serverTimezone` / `connectionTimeZone` in the JDBC URL as verified in the MySQL Connector/J docs) so `CURDATE()` and `LocalDate.now()` agree.

**Cloud deployment (document in README, do not automate):** Ubuntu LTS VM with at least 2 GB RAM (add swap on 1 GB); firewall 80/443 open, 22 from team IP only; install Docker Engine and the Compose plugin from Docker's official docs; `git clone`; create `.env`; `docker compose up -d --build`; check `docker compose ps` and `/api/actuator/health`; optional domain + Let's Encrypt; daily `mysqldump` backup; billing alert; stop the VM when unused.

**Optional CI:** GitHub Actions running `mvn -q verify` and `docker compose build` on push.

---

## 16. Testing

### 16.1 Required unit tests (engine classes, no Spring context)

Each worked example in Section 9 must be a test with the exact expected values:

| Test | Expected |
| --- | --- |
| Overlap boundaries (R1) | 1–10 & 10–20 overlap; 1–10 & 11–20 do not; single-day ranges; identical ranges |
| Example 1 over-allocation | 110% period on the overlapping days, CRITICAL, peak 110 |
| Exactly 100% | no conflict |
| Half-time person (capacity 50) at 70% | WARNING |
| Capacity 0 / INACTIVE | any load > 0 is a conflict; INACTIVE_RESOURCE raised |
| Weekend | developer capacity 0 on Sat/Sun; hardware not affected |
| Leave (Example 4) | LEAVE_CONFLICT for leave days; REJECTED leave has no effect |
| Leave over two projects | two LEAVE_CONFLICTs |
| AC1 example | load 75, remaining 25 |
| Priya example | load 80, remaining 20, no conflict |
| Skill match | 66.67 with missing [MySQL]; 100 when no requirements; partial credit 1/2 = 0.5 |
| Skill gap | AWS 3 required, holders only at level 2 → CRITICAL SKILL_GAP; holder exists but fully booked → WARNING |
| Ranking | Karthik 0.800, Arun 0.683 (±0.001), order Karthik then Arun; equal rates → costNorm 0 |
| Burnout | 90, 95, 88 → risk; 90, 95, 80 → none |
| Budget Example 5 | remaining 200000.00, utilisation 60.00, burn 10000.00, daysLeft 20, runOut = today + 20 |
| Budget edge cases | no transactions → INSUFFICIENT_DATA; burn 0 → INSUFFICIENT_DATA; project younger than window; remaining 0; remaining negative → EXCEEDED |
| EVM | CPI 0.9333, SPI 0.875, EAC 535714.29; CPI null when actual 0 |
| Delivery | 240 h at 8 h/day → 30 working days; leave day adds a day; no allocations → NO_CAPACITY; weekend skipped |
| Swap Example 6 | 40 vs 30 days; ₹96,000 vs ₹93,600; B at 110% → DO_NOT_APPLY; conflict removed for A when applicable |
| Simulation immutability | allocation checksum unchanged after run and after save |
| Dedup/resolve | same conflict twice → one row; conflict no longer present → RESOLVED + one INFO notification |

### 16.2 Integration tests (Spring Boot Test against MySQL — Testcontainers if Docker is available, otherwise a dedicated test schema)

- Allocation create → 409 on over-allocation without override; 201 with override and a stored CRITICAL conflict and notifications for PM and developer.
- Validation errors → 400 with `fieldErrors`.
- Security: unauthenticated → 401; DEVELOPER on `POST /api/projects` → 403; DEVELOPER reading another developer's data → 403; CSRF missing → 403.
- Budget forecast endpoint returns Example 5 numbers for seeded-style data.
- Simulation endpoints leave `allocation` unchanged.

### 16.3 Edge cases that must be handled (and tested where marked T)

1. Developer with 0% capacity (T). 2. Planned leave (T). 3. Multiple overlapping milestones (T). 4. Exactly 100% (T). 5. 100% capacity, 110% load (T). 6. Milestone with no developer (T). 7. Skill nobody has (T). 8. Project with no transactions (T). 9. Burn rate zero (T). 10. Budget exactly exhausted (T). 11. Budget exceeded (T). 12. Milestone end before start → 400 (T). 13. Leave overlapping multiple projects (T). 14. Swap creates a conflict (T). 15. Swap removes a conflict (T). 16. Inactive developer (T). 17. Completed project/milestone ignored (T). 18. Allocation deleted → conflicts resolved (T). 19. Milestones overlapping by one day (T). 20. Ranges touching end/start without overlap (T). 21. Allocation outside milestone dates → 400. 22. Duplicate allocation → 409. 23. Ranking weights not summing to 1 → 400. 24. Forecast loop cap reached → NO_CAPACITY. 25. Half-time developer over capacity but ≤ 100 → WARNING (T).

Never write a test that only restates a hard-coded output without exercising the rule.

---

## 17. Documentation deliverables

1. `README.md`: overview, problem, features, architecture (with diagram image or link), tech stack, prerequisites, setup (Docker and local dev), demo credentials, API summary, how to run tests, Postman usage, cloud deployment steps, troubleshooting (port in use, seed not reloading, schema validation errors, 401/403/CSRF), limitations, future enhancements, screenshots placeholders clearly marked.
2. `docs/ARCHITECTURE.md`: layers, engine design, request journey (allocation create), conflict engine flow, simulation flow.
3. `docs/ER_DIAGRAM.md` and `docs/er-diagram.drawio`, `docs/architecture.drawio`: generated from the actual schema (valid Draw.io XML that opens in diagrams.net).
4. `postman/PRS.postman_collection.json`: folders Auth, Projects, Milestones, Resources, Allocations, Leave, Budget, Conflicts, Timeline/Heatmap, Simulation, Dashboards, Notifications, Admin; login request stores the session and CSRF token in collection variables; each important request has an example response and a test script asserting the status code.
5. `docs/PROGRESS.md`, `docs/DECISIONS.md` kept current.
6. `ACCEPTANCE_CRITERIA_CHECKLIST.md` (Section 19).

**Limitations to state honestly (README and paper):** linear, non-learning forecasts; public holidays and part-day leave not modelled; ranking weights set by hand; delivery speed factor is an assumption; cloud cost entered or CSV-imported, not live; evaluation on synthetic data; single organisation and time zone.

---

## 18. Build phases (do ONE at a time; stop after each)

| Phase | Work | Done when |
| --- | --- | --- |
| 0 Inspect | Inspect the workspace; list existing files; confirm the plan; create `docs/PROGRESS.md`, `docs/DECISIONS.md`, `.gitignore`, `.env.example` | Report lists what exists and the file plan; no code yet |
| 1 Schema | `01-schema.sql` (18 tables, all constraints and indexes) and `settings` rows | Script runs cleanly on `mysql:8.0` in Docker; `SHOW CREATE TABLE` output matches Section 7 |
| 2 Backend skeleton | Spring Boot project from Initializr; packages; entities; repositories; envelope; exception handler; `ddl-auto=validate` | App starts against the schema with no validation errors; `mvn -q verify` passes |
| 3 Security and CRUD | SecurityFilterChain, login/logout/me, CSRF, roles; CRUD for projects, milestones, skills, resources, skill requirements, allocations (without conflict logic yet), users (admin) | Postman: login works; CRUD works; 401/403 cases correct; integration tests pass |
| 4 Engine core | `util/DateRanges`, `WorkingDays`; engine CapacityCalculator, ConflictRules (all types except budget/delivery), SkillMatcher (match, gap, ranking), burnout; SettingsService | All 16.1 tests for these parts pass with the exact expected values |
| 5 Conflict service + seed | ConflictDetectionService (sync + scan + upsert/resolve), allocation 409/override and skill-mismatch flow, leave workflow, notifications; `02-data.sql`; startup scan | Every intended seed situation in Section 14 appears; no unintended CRITICALs; notification rows exist for PM and developers |
| 6 Budget and delivery | BudgetCalculator (planned, actual, burn, forecast, EVM, series), DeliveryForecaster, BUDGET_RISK and DELIVERY conflicts; endpoints | Example 5, EVM and delivery tests pass; ALPHA shows run-out T+20 |
| 7 Simulation and PDF | SimulationService (run, save, get), recommendation, ReportService PDF | Example 6 test passes; immutability test passes; PDF opens and every number matches the saved result |
| 8 Dashboards, timeline, heatmap, matrix APIs | Dashboard payloads, timeline, heatmap, matrix, candidates endpoints | Postman responses match Section 10.3 shapes, values traceable to seed data |
| 9 Frontend | Shared JS/CSS; all 15 pages wired to the API | Every page works with seed data at 360 px and 1440 px; no hard-coded data (grep check); console free of errors |
| 10 Scheduler and polish | Nightly job; bell polling; empty/loading/error states; accessibility checks | Manual trigger of the job works; states visible |
| 11 Docker | Dockerfile, compose, nginx.conf, time zones | On a clean clone: `docker compose up --build` → login page at http://localhost, dashboard populated |
| 12 Docs and Postman | README, ARCHITECTURE, ER docs, Draw.io files, Postman collection | Postman collection run passes; Draw.io files open |
| 13 Final audit | Full test run; Section 19 checklist; fix every FAIL | All AC PASS with evidence |

---

## 19. Final acceptance audit

Create `ACCEPTANCE_CRITERIA_CHECKLIST.md` with one row per criterion: ID, criterion, PASS/FAIL, evidence (test name, endpoint + Postman result, page), implementing files.

| AC | Criterion | How to verify |
| --- | --- | --- |
| AC1 | Profiles: skills, hourly cost, % bandwidth across active projects, current load, remaining | `GET /api/resources/{arun}`; Resources page |
| AC2 | Over 100% across simultaneous milestones flagged | Seed allocation #1+#2 → CRITICAL in Conflict Center; unit test |
| AC3 | Swap simulation with instant date and cost recalculation; no mutation | Simulator page with swap #2 → Karthik; immutability test |
| AC4 | Timeline dashboard: health, milestone progress, utilisation heatmap | PM dashboard, timeline, heatmap pages |
| AC5 | Skill gap alerts; ranking | GAMMA M2 SKILL_GAP; candidates endpoint |
| AC6 | Budget burn-down and forecast | ALPHA budget page (run-out T+20, charts) |
| AC7 | Leave/maintenance reduce capacity | Rahul LEAVE_CONFLICT |
| AC8 | In-app notifications | Bell count; notifications page |
| AC9 | What-if PDF export | PDF from saved scenario |
| AC10 | Role-based dashboards enforced by server | Developer dashboard; 403 tests |
| AC11 | Hardware and cloud conflicts | GPU OVER_ALLOCATION; CLOUD_QUOTA |
| AC12 | Burnout warning | Priya BURNOUT_RISK |

**Final verification list:** app starts; DB connects; schema validates; seed loads; backend compiles; all tests pass; frontend loads; PM and developer dashboards work; allocation, over-allocation, skill gap, leave, budget, forecast, timeline, heatmap, notifications, simulation (non-mutating), PDF, Postman, validation, error handling, responsive UI, README complete. Do not declare completion while anything important is broken. If something cannot be done exactly, document the limitation and implement the closest reliable solution.

---

## 20. Clarifications (these win over earlier text)

1. `resource.status` is only ACTIVE or INACTIVE. A retired machine is INACTIVE. Hardware maintenance is an `unavailability` row of type MAINTENANCE, not a status.
2. The weekend rule (R3) applies to DEVELOPER resources only. HARDWARE and CLOUD use calendar days.
3. Allocations are hard-deleted (`DELETE` → 204) and have no status column; history of decisions is kept in conflicts, notifications and saved scenarios. Projects are archived, never deleted.
4. Milestone `status` is set by the PM. Risk is shown separately through conflicts and the health badge (9.9); the system does not overwrite the PM's status.
5. Ranking example: exact value for Arun is 0.683 (using match 2/3); 0.69 appears in older documents because they rounded 0.67 first. Tests use 0.683 ± 0.001.
6. Worked Example 1 uses calendar dates for explanation. For a DEVELOPER, over-allocation is evaluated only on available working days (Rule R4), so the reported period starts and ends on weekdays (for 2027, 10 Jan is a Sunday, so a DEVELOPER period would be 11–20 Jan). Tests must choose dates and assertions accordingly and say so.
7. `AllocationService` handles creating, updating and deleting allocations with Rules R2 and R6; it calls the conflict engine after commit-relevant changes.
8. The `ProjectBudget` entity from the original prompt is merged into `project.total_budget`; `Leave` is the LEAVE type of `unavailability`.
9. The original prompt's route `GET /api/dashboard/developer/{developerId}` is replaced by `/api/dashboard/developer/me` for security.
10. Anything not specified here is decided by the simplest option, recorded in `docs/DECISIONS.md`, and reported.
