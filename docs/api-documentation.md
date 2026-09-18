# REST API Documentation

**Base URL:** `http://localhost:8080`
**Content type:** `application/json` (except the PDF report, which is `application/pdf`)
**Dates:** always `yyyy-MM-dd`, for example `2026-09-18`

There are two ways to try these endpoints:

* **In the browser** — start the application and open
  <http://localhost:8080/api.html> (or the **API Explorer** item in the menu).
  Every endpoint below is listed there and can be sent with one click, with
  the JSON body prefilled and editable.
* **In Postman** — import
  `postman/Project-Resource-Allocation-System.postman_collection.json`.

---

## Error format

Every error — validation or not found — comes back in the same shape:

```json
{
  "timestamp": "2026-09-18T10:15:30",
  "status": 400,
  "error": "Validation Failed",
  "messages": ["Allocation percentage must be between 1 and 100"]
}
```

| Status | When |
|---|---|
| 200 OK | A successful GET, PUT or a simulation run |
| 201 Created | A successful POST that created a row |
| 204 No Content | A successful DELETE |
| 400 Bad Request | A broken rule, a bad value, or malformed JSON |
| 404 Not Found | An id that does not exist, or an unknown page |
| 500 Internal Server Error | An unexpected fault (still JSON, never a stack trace) |

---

## 1. Login

### `POST /api/login`
Decides which dashboard the browser should open. It does not create a session.

Request:
```json
{ "username": "manager", "password": "manager123" }
```
Response `200`:
```json
{ "userId": 1, "username": "manager", "role": "PROJECT_MANAGER",
  "developerId": null, "developerName": null }
```
A developer account returns `"role": "DEVELOPER"` with their `developerId`.
A wrong password returns `400` with `"Invalid username or password"`.

---

## 2. Projects

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/projects` | All projects |
| GET | `/api/projects/{id}` | One project |
| POST | `/api/projects` | Create — also creates the project's budget row |
| PUT | `/api/projects/{id}` | Update |
| DELETE | `/api/projects/{id}` | Delete (cascades to milestones, allocations, budget) |
| GET | `/api/projects/{id}/milestones` | That project's milestones |
| GET | `/api/projects/{id}/timeline` | Project plus milestones, ready to draw |

Request body for POST and PUT:
```json
{
  "name": "Hospital Management Portal",
  "description": "Patient records, appointments and billing.",
  "startDate": "2026-09-01",
  "endDate": "2026-12-31",
  "budget": 500000,
  "status": "IN_PROGRESS"
}
```
`status` is one of `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `AT_RISK`
(optional — defaults to `PLANNED`).

Response `201`:
```json
{ "id": 1, "name": "Hospital Management Portal", "description": "...",
  "startDate": "2026-09-01", "endDate": "2026-12-31",
  "budget": 500000.00, "status": "IN_PROGRESS",
  "milestoneCount": 3, "allocationCount": 4 }
```

Validation: name required · budget not negative · `endDate >= startDate`.

---

## 3. Developers

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/developers` | All resource profiles |
| GET | `/api/developers/{id}` | One developer |
| POST | `/api/developers` | Create |
| PUT | `/api/developers/{id}` | Update (a non-empty `skills` list replaces the old one) |
| DELETE | `/api/developers/{id}` | Delete |
| POST | `/api/developers/{id}/skills` | Add one skill |
| DELETE | `/api/developers/{id}/skills/{skillId}` | Remove one skill |
| GET | `/api/developers/{id}/allocations` | That developer's allocations |
| GET | `/api/developers/{id}/leaves` | That developer's leave |
| GET | `/api/developers/{id}/dashboard` | The developer dashboard |

Request body:
```json
{
  "name": "Arun",
  "email": "arun@sist.edu",
  "role": "Backend Developer",
  "hourlyCost": 500,
  "availabilityPercentage": 100,
  "status": "ACTIVE",
  "skills": ["Java", "Spring Boot", "MySQL"]
}
```
Skill names that do not exist yet are created automatically.

Adding one skill — `POST /api/developers/1/skills`:
```json
{ "skillName": "AWS", "proficiencyLevel": "EXPERT" }
```

Validation: name required · valid email · email unique · hourly cost not
negative · availability between 0 and 100.

---

## 4. Skills

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/skills` | The master skill list |
| GET | `/api/skills/{id}` | One skill |
| POST | `/api/skills` | Create — body `{ "name": "Kotlin", "category": "Backend" }` |
| DELETE | `/api/skills/{id}` | Delete |

Skill names are unique, case-insensitively.

---

## 5. Milestones

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/milestones` | All milestones |
| GET | `/api/milestones?projectId=1` | Filtered by project |
| GET | `/api/milestones/{id}` | One milestone |
| POST | `/api/milestones` | Create |
| PUT | `/api/milestones/{id}` | Update |
| DELETE | `/api/milestones/{id}` | Delete |
| POST | `/api/milestones/{id}/skills` | Add a required skill — `{ "skillName": "AWS" }` |
| DELETE | `/api/milestones/{id}/skills/{skillId}` | Remove a required skill |

Request body:
```json
{
  "projectId": 1,
  "name": "Backend API Development",
  "description": "REST APIs for patients, doctors and billing.",
  "startDate": "2026-09-01",
  "endDate": "2026-09-30",
  "progressPercentage": 40,
  "status": "IN_PROGRESS",
  "estimatedEffortDays": 20,
  "requiredSkills": ["Java", "Spring Boot", "MySQL"]
}
```

Response adds three calculated fields:
```json
{ "...": "...",
  "assignedDevelopers": ["Arun (60%)", "Divya (50%)"],
  "totalAllocationPercentage": 110,
  "estimatedDeliveryDate": "2026-09-19" }
```

`estimatedDeliveryDate` = `startDate + ceil(estimatedEffortDays / (totalAllocation / 100)) - 1`.
It is `null` when nobody is allocated.

Validation: name required · `endDate >= startDate` · effort at least 1
developer-day · the milestone must sit inside its project's dates.

---

## 6. Allocations

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/allocations` | All allocations |
| GET | `/api/allocations?developerId=3` | Filtered by developer |
| GET | `/api/allocations?projectId=1` | Filtered by project |
| GET | `/api/allocations/{id}` | One allocation |
| POST | `/api/allocations` | Create |
| PUT | `/api/allocations/{id}` | Update |
| DELETE | `/api/allocations/{id}` | Delete |

Request body (`milestoneId` may be `null` for whole-project work):
```json
{
  "developerId": 1,
  "projectId": 1,
  "milestoneId": 1,
  "startDate": "2026-09-01",
  "endDate": "2026-09-30",
  "allocationPercentage": 60
}
```

**The response carries a `warnings` list.** The allocation is saved, but any
skill gap or conflict it causes is reported straight back, so an assignment
is never silent:

```json
{
  "id": 8, "developerName": "Priya", "projectName": "Hospital Management Portal",
  "milestoneName": "Backend API Development", "allocationPercentage": 30,
  "warnings": [
    "SKILL GAP: Priya does not have the required skill(s) Java, Spring Boot, MySQL for milestone Backend API Development.",
    "CRITICAL: Priya is allocated at 110% between September 5, 2026 and September 30, 2026, but is only 100% available."
  ]
}
```
An empty `warnings` array means a clean allocation.

Validation: percentage between 1 and 100 · `endDate >= startDate` · dates
inside the project *and* inside the milestone · the milestone must belong to
the project · an `INACTIVE` developer cannot be allocated.

---

## 7. Leave

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/leaves` | All leave |
| GET | `/api/leaves?developerId=2` | Filtered by developer |
| POST | `/api/leaves` | Record leave |
| DELETE | `/api/leaves/{id}` | Delete |

```json
{ "developerId": 2, "startDate": "2026-09-10", "endDate": "2026-09-14",
  "reason": "Family function", "status": "APPROVED" }
```
`status` is `PLANNED`, `APPROVED` or `CANCELLED`. **Cancelled leave is
ignored by conflict detection**; any other leave overlapping an allocation
produces a LEAVE CONFLICT.

---

## 8. Conflicts, skill gaps and utilization

### `GET /api/conflicts` — optionally `?developerId=3`

```json
[{
  "type": "OVER_ALLOCATION",
  "severity": "CRITICAL",
  "developerId": 3,
  "developerName": "Rahul",
  "startDate": "2026-09-15",
  "endDate": "2026-10-15",
  "totalAllocation": 110,
  "availability": 100,
  "message": "CRITICAL: Rahul is allocated at 110% between September 15, 2026 and October 15, 2026, but is only 100% available.",
  "involvedAllocations": [
    "Hospital Management Portal / Frontend Development (60%)",
    "E-Commerce Analytics Platform / Reporting Dashboard (50%)"
  ]
}]
```
`type` is `OVER_ALLOCATION` or `LEAVE_CONFLICT`. Only allocations whose dates
overlap are added together, and the reported period is the overlap itself.

### `GET /api/skill-gaps`

```json
[{
  "milestoneId": 3,
  "milestoneName": "Cloud Deployment",
  "projectName": "Hospital Management Portal",
  "requiredSkills": ["AWS", "Azure"],
  "coveredSkills": ["AWS"],
  "missingSkills": ["Azure"],
  "suggestions": ["Azure - Skill Gap: No available developer has this skill."],
  "message": "SKILL GAP: Cloud Deployment is missing Azure."
}]
```
When somebody in the company does have the missing skill, the suggestion
names them instead: `"AWS - available developers: Karthik"`.

### `GET /api/utilization`

```json
[{ "developerId": 3, "developerName": "Rahul",
   "peakAllocation": 110, "currentAllocation": 110,
   "availability": 100, "remainingCapacity": 0,
   "status": "Conflict", "onLeaveToday": false }]
```
`peakAllocation` is the highest *simultaneous* allocation, so a developer who
is free today but double-booked next month still shows as `Conflict`.
`status` is `Available`, `High Load` (≥ 80% of availability),
`Fully Allocated` or `Conflict`.

---

## 9. Budget

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/projects/{id}/budget` | Budget summary |
| GET | `/api/projects/{id}/budget/forecast` | Burn-rate forecast |
| GET | `/api/projects/{id}/budget/transactions` | Spending entries |
| POST | `/api/projects/{id}/budget/transactions` | Record spending |

### Budget summary
```json
{ "projectId": 1, "projectName": "Hospital Management Portal",
  "totalBudget": 500000.00, "currentSpending": 300000.00,
  "remainingBudget": 200000.00, "utilizationPercentage": 60.0,
  "hasSpendingData": true, "warningLevel": "NONE",
  "message": "Budget usage is within the safe range." }
```
`warningLevel` is `CRITICAL` at 90% or above, `WARNING` at 75% or above,
otherwise `NONE`.

**No division ever happens when it cannot.** With no budget or no spending,
`utilizationPercentage` is `null` and the message explains why:
```json
{ "utilizationPercentage": null, "hasSpendingData": false,
  "message": "No spending data available" }
```

### Forecast
```json
{ "totalBudget": 500000.00, "currentSpending": 300000.00,
  "remainingBudget": 200000.00, "burnRatePerDay": 10000.00,
  "daysElapsed": 30, "estimatedDaysRemaining": 20,
  "estimatedExhaustionDate": "2026-10-08", "hasSpendingData": true,
  "message": "At the current burn rate, the project is estimated to exhaust its budget in approximately 20 days.",
  "methodNote": "This forecast is a simple calculation: remaining budget divided by the current burn rate. It is not an AI prediction." }
```

The arithmetic:
```
daysElapsed = days from the first spending entry to today (at least 1)
burnRate    = currentSpending / daysElapsed
daysLeft    = remainingBudget / burnRate      (rounded down)
```

### Record spending
```json
{ "transactionDate": "2026-09-18", "amount": 25000,
  "category": "Salary", "description": "Extra sprint" }
```
This also updates `project_budgets.current_spending`. The amount must be
greater than zero.

---

## 10. Notifications

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/notifications` | All alerts, newest first |
| GET | `/api/notifications?developerId=3` | One developer's alerts |
| GET | `/api/notifications/unread-count` | `{ "unread": 5 }` |
| POST | `/api/notifications/refresh` | Re-scan the system and store any new alerts |
| PUT | `/api/notifications/{id}/read` | Mark one as read |
| DELETE | `/api/notifications` | Clear them all |

`type` is one of `OVER_ALLOCATION`, `SKILL_GAP`, `LEAVE_CONFLICT`,
`BUDGET_RISK`, `MILESTONE_DELAY`, `MILESTONE_DEADLINE`.
`severity` is `INFO`, `WARNING` or `CRITICAL`.

`refresh` is safe to call repeatedly — an identical message is never stored
twice. The manager dashboard calls it on every load.

---

## 11. What-if simulator

### `POST /api/simulations/run` — calculate only, **writes nothing**

```json
{ "milestoneId": 2, "currentDeveloperId": 3,
  "newDeveloperId": 4, "newAllocationPercentage": 50 }
```
`newAllocationPercentage` is optional; leaving it out reuses the current
developer's percentage.

Response `200`:
```json
{
  "id": null, "saved": false,
  "projectName": "Hospital Management Portal",
  "milestoneName": "Frontend Development",
  "currentDeveloperName": "Rahul",   "newDeveloperName": "Divya",
  "currentHourlyCost": 450.00,       "newHourlyCost": 550.00,
  "currentAllocationPercentage": 60, "simulatedAllocationPercentage": 50,
  "currentCost": 56160.00,           "simulatedCost": 57200.00,
  "budgetImpact": 1040.00,
  "currentSkillMatch": 100,          "simulatedSkillMatch": 0,
  "currentMissingSkills": [],        "simulatedMissingSkills": ["JavaScript", "HTML", "CSS"],
  "currentConflicts": 1,             "simulatedConflicts": 0,
  "simulatedConflictDetails": [],
  "currentDeliveryDate": "2026-10-26",
  "simulatedDeliveryDate": "2026-11-03",
  "deliveryImpactDays": 8,
  "deliveryImpactMessage": "Delivery moves 8 day(s) LATER.",
  "recommendation": "NOT RECOMMENDED: skill match drops from 100% to 0%. Missing: JavaScript, HTML, CSS"
}
```

`"id": null` and `"saved": false` prove nothing was stored. You can confirm
it by calling `GET /api/simulations` straight afterwards — the list is
unchanged.

### `POST /api/simulations` — save the scenario
The same body plus a `scenarioName`. This is the only call that writes to
`simulation_scenarios`. Returns `201` with `"saved": true` and an `id`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/simulations` | Saved scenarios |
| GET | `/api/simulations/{id}` | One saved scenario |
| DELETE | `/api/simulations/{id}` | Delete a scenario |
| GET | `/api/simulations/{id}/report` | PDF of a saved scenario |
| POST | `/api/simulations/report` | PDF of an unsaved scenario (stores nothing) |

Both report endpoints return `application/pdf` with a
`Content-Disposition: attachment` header.

Validation: the replacement must differ from the current developer, and the
current developer must actually be allocated to that milestone.

---

## 12. Dashboards and timeline

### `GET /api/dashboard/summary`
Everything the Project Manager dashboard draws, in one response:
counts (`totalProjects`, `activeProjects`, `atRiskProjects`,
`totalDevelopers`), `averageUtilization`, conflict and skill-gap counts,
budget totals, and the lists `utilization`, `conflicts`, `skillGapDetails`,
`upcomingMilestones`, `budgetWarnings` and `notifications`.

### `GET /api/dashboard/developer/{developerId}`
The simpler developer view: `myProjects`, `myMilestones`, `myAllocations`,
`currentAllocation`, `availableCapacity`, `upcomingDeadlines`, `myLeave`,
`myConflicts` and `myNotifications`.

### `GET /api/timeline`
Every project with its milestones. `GET /api/projects/{id}/timeline` returns
just one.

---

## 13. Quick reference

```
POST   /api/login

GET    /api/projects                       POST   /api/projects
GET    /api/projects/{id}                  PUT    /api/projects/{id}
DELETE /api/projects/{id}
GET    /api/projects/{id}/milestones
GET    /api/projects/{id}/timeline
GET    /api/projects/{id}/budget
GET    /api/projects/{id}/budget/forecast
GET    /api/projects/{id}/budget/transactions
POST   /api/projects/{id}/budget/transactions

GET    /api/developers                     POST   /api/developers
GET    /api/developers/{id}                PUT    /api/developers/{id}
DELETE /api/developers/{id}
POST   /api/developers/{id}/skills
DELETE /api/developers/{id}/skills/{skillId}
GET    /api/developers/{id}/allocations
GET    /api/developers/{id}/leaves
GET    /api/developers/{id}/dashboard

GET    /api/skills                         POST   /api/skills
GET    /api/skills/{id}                    DELETE /api/skills/{id}

GET    /api/milestones                     POST   /api/milestones
GET    /api/milestones/{id}                PUT    /api/milestones/{id}
DELETE /api/milestones/{id}
POST   /api/milestones/{id}/skills
DELETE /api/milestones/{id}/skills/{skillId}

GET    /api/allocations                    POST   /api/allocations
GET    /api/allocations/{id}               PUT    /api/allocations/{id}
DELETE /api/allocations/{id}

GET    /api/leaves                         POST   /api/leaves
DELETE /api/leaves/{id}

GET    /api/conflicts
GET    /api/skill-gaps
GET    /api/utilization

GET    /api/notifications                  POST   /api/notifications/refresh
GET    /api/notifications/unread-count     PUT    /api/notifications/{id}/read
DELETE /api/notifications

POST   /api/simulations/run                POST   /api/simulations
GET    /api/simulations                    GET    /api/simulations/{id}
DELETE /api/simulations/{id}
GET    /api/simulations/{id}/report        POST   /api/simulations/report

GET    /api/dashboard/summary
GET    /api/dashboard/developer/{id}
GET    /api/timeline
```
