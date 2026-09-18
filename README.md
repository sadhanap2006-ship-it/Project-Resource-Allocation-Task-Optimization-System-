# Project Resource Allocation System

**SIST CSE Capstone Project**

A web application that helps an IT project manager allocate developers, lab
hardware and cloud budget across overlapping projects — and warns them when
somebody is double-booked, missing a required skill, on leave, or about to
run out of money.

---

## The problem

Project managers juggle several projects at once. Without a single view of
who is working on what, they get developer over-allocation and burnout,
skill mismatches, resource conflicts, missed milestones and budget overruns.
This system puts projects, people, milestones, allocations, leave and budget
in one place, and applies clear rules to point out the problems.

---

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, vanilla JavaScript (no framework) |
| Backend | Java 21, Spring Boot 3.3.5, REST API |
| Database | MySQL 8.0 |
| Build | Maven |
| Testing | JUnit 5, Mockito, Postman |
| Documentation | Markdown, Draw.io |

The only library outside this list is **OpenPDF**, used solely for the PDF
export. It is explained — including how to do without it — in
[`docs/architecture.md`](docs/architecture.md#7-the-one-extra-dependency-openpdf).

---

## Features

1. Resource profiles (name, role, hourly cost, availability, status)
2. Developer skills with proficiency levels
3. Project management
4. Milestone management with required skills
5. Resource allocation
6. Over-allocation conflict detection
7. Skill-gap detection
8. Leave and availability management
9. Budget tracking
10. Budget forecasting from the current burn rate
11. Timeline visualisation
12. Resource utilization view
13. In-app notifications
14. What-if resource swap simulation
15. Role-based dashboards (manager and developer)
16. PDF report export
17. A built-in REST API explorer page, so the API can be demonstrated in the browser

---

## Getting started

### Prerequisites

* JDK 21 or newer — `java -version`
* Maven 3.9 or newer — `mvn -version`
* MySQL 8.0, running on `localhost:3306`

### 1. Create the database

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample-data.sql
```

The first script drops and recreates `resource_allocation_db` with all 13
tables. The second fills it with demo data.

### 2. Point the application at your MySQL

Edit `backend/src/main/resources/application.properties` if your username or
password differ from the defaults:

```properties
spring.datasource.username=root
spring.datasource.password=root
```

### 3. Run the application

```bash
cd backend
mvn spring-boot:run
```

Or build a jar and run that:

```bash
mvn clean package
java -jar target/resource-allocation-system-1.0.0.jar
```

### 4. Open it

<http://localhost:8080>

Spring Boot serves the pages in `frontend/` itself, so there is nothing else
to start.

### Letting somebody else open it

`http://localhost:8080` only works on the machine running the application.
To let an examiner reach it — either a temporary public link from your own
laptop, or proper hosting — follow
[`docs/deployment.md`](docs/deployment.md). Read its first section before
you share any address: the API has no authentication until you switch the
password gate on.

### Demo logins

| Username | Password | Opens |
|---|---|---|
| `manager` | `manager123` | Project Manager dashboard |
| `arun` | `arun123` | Developer dashboard |
| `priya` | `priya123` | Developer dashboard |
| `rahul` | `rahul123` | Developer dashboard (has a conflict) |
| `divya` | `divya123` | Developer dashboard |
| `karthik` | `karthik123` | Developer dashboard |

> The demo passwords are stored as plain text. A production system would
> store a BCrypt hash — see the security note in `docs/architecture.md`.

---

## What the sample data shows immediately

Log in as `manager` and the dashboard already demonstrates every rule:

| What to look for | Where |
|---|---|
| **Over-allocation conflict** — Rahul at 110% (60% + 50% on overlapping dates) | Dashboard and Conflicts, red row |
| **Skill gap, nobody available** — "Cloud Deployment" needs Azure | Conflicts → Skill gaps |
| **Skill gap with a suggestion** — "Data Migration" needs Java and MySQL | Conflicts → Skill gaps |
| **Leave conflict** — Priya is 80% allocated during her 10–14 Sep leave | Conflicts, amber row |
| **Budget warning** — E-Commerce project is 90% spent | Budget, red row |
| **Budget forecast** — "approximately 20 days" | Budget → Forecast |
| **No spending data guard** — College ERP has no spending | Budget, third row |
| **A clean allocation** — Arun at 60%, full skill match, no conflict | Allocations |
| **Non-overlapping work is fine** — Divya works Sept and late Oct | Conflicts (she does not appear) |
| **The REST API itself**, endpoint by endpoint | API Explorer |

---

## Project structure

```
project-resource-allocation-system/
├── implementation_plan.md          the plan this project was built from
├── README.md
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/sist/rms/
│       │   ├── controller/         REST endpoints
│       │   ├── service/            all the business rules
│       │   ├── repository/         Spring Data JPA interfaces
│       │   ├── entity/             one class per table
│       │   ├── dto/                request and response objects
│       │   ├── exception/          404 / 400 handling
│       │   └── config/             CORS and static file serving
│       ├── main/resources/application.properties
│       └── test/java/com/sist/rms/service/    39 JUnit tests
├── frontend/
│   ├── index.html  dashboard.html  projects.html  developers.html
│   ├── allocations.html  conflicts.html  budget.html  timeline.html
│   ├── simulator.html  developer-dashboard.html  api.html
│   ├── css/style.css
│   └── js/api.js + one script per page
├── Dockerfile                      used by hosting services (see docs/deployment.md)
├── database/
│   ├── schema.sql                  13 tables
│   ├── sample-data.sql             demo data
│   ├── schema-hosted.sql           same tables, for a managed MySQL
│   └── sample-data-hosted.sql      same data, for a managed MySQL
├── docs/
│   ├── architecture.md
│   ├── database-design.md
│   └── api-documentation.md
└── postman/
    └── Project-Resource-Allocation-System.postman_collection.json
```

---

## The business rules, in plain words

### Over-allocation — only overlapping work competes

Adding up all of a developer's allocations would be wrong, because work in
January does not compete with work in March. So the system does a
**timeline sweep**:

1. collect every boundary date — each start date, and each end date + 1 day
2. sort them; between two consecutive boundaries the set of running
   allocations never changes
3. add up the allocations running in each of those stretches
4. any stretch whose total exceeds the developer's availability is an
   **OVER-ALLOCATION CONFLICT**

| Project A | Project B | Dates | Total | Result |
|---|---|---|---|---|
| 60% | 50% | same | 110% | CONFLICT |
| 60% | 40% | same | 100% | fine |
| 60% | 50% | different | never at once | fine |

### Leave conflict

During approved leave the effective availability is 0%, so any allocation
overlapping a leave period is a **LEAVE CONFLICT**. Cancelled leave is
ignored.

### Skill gap

```
required = the skills listed for the milestone
covered  = every skill owned by the developers allocated to it
missing  = required − covered
```

For each missing skill the system looks for any active developer who has it.
If somebody does, it suggests them. If nobody does, it says exactly:

> Skill Gap: No available developer has this skill.

Assigning a developer who lacks a required skill is still allowed — a manager
may want a trainee — but it is **never silent**: the API returns a warning and
records a notification.

### Budget

```
remaining budget = total budget − current spending
utilization (%)  = (current spending / total budget) × 100
```

If the budget is zero, or nothing has been spent, no division happens at all
and the screen shows **"No spending data available"**.
Warnings: 90% or more is CRITICAL, 75% or more is a WARNING.

### Budget forecast — arithmetic, not AI

```
days elapsed = days from the first spending entry until today (at least 1)
burn rate    = current spending / days elapsed
days left    = remaining budget / burn rate        (rounded down)
```

Worked example, straight from the sample data:

```
budget ₹500,000 · spent ₹300,000 · remaining ₹200,000
burn rate ₹10,000/day → 200,000 / 10,000 = 20 days
```

> "At the current burn rate, the project is estimated to exhaust its budget
> in approximately 20 days."

This is a plain calculation on the current burn rate. It is **not** a
prediction from any AI or machine-learning model, and the screen says so.

### Delivery date

Each milestone stores `estimatedEffortDays` in **developer-days** — 20
developer-days means one full-time developer would need 20 days.

```
capacity      = (sum of allocation percentages on the milestone) / 100
duration_days = ceil(estimatedEffortDays / capacity)
delivery_date = milestone start + duration_days − 1
```

* less capacity → longer duration → later delivery
* more capacity → shorter duration → earlier delivery

For 20 developer-days: one developer at 100% takes 20 days, at 50% takes 40
days, and two developers at 100% take 10 days. With nobody allocated the
capacity is 0, so no date is estimated — never a division by zero.

### Cost

```
working days = Monday–Friday days in the period
hours        = working days × 8 × (allocation % / 100)
cost         = hours × hourly rate
```

### Skill match

```
skill match (%) = (required skills the developer has / total required) × 100
```

---

## The what-if simulator

Pick a milestone, the developer currently on it and a replacement. The system
reports, side by side: resource, hourly cost, allocation, cost, skill match,
missing skills, conflicts, estimated delivery date and budget impact — plus a
plain-English recommendation.

**Running a simulation changes nothing.** `POST /api/simulations/run` performs
no insert, update or delete; every number is worked out in memory from a copy
of the data. A row is written to `simulation_scenarios` only when you press
**Save this scenario**. You can prove it: run a simulation, then call
`GET /api/simulations` — the list is unchanged.

The report can be downloaded as a PDF, either from a saved scenario or
directly from an unsaved one.

---

## Testing

### Unit tests

```bash
cd backend
mvn test
```

39 JUnit 5 tests, covering every case in the specification:

| Test class | Covers |
|---|---|
| `ConflictServiceTest` | Cases 1, 2, 3 and 5, part-time availability, and that a conflict covers only the overlapping days |
| `BudgetServiceTest` | Case 6, the 75% and 90% thresholds, zero budget, zero spending, an exhausted budget |
| `SkillGapServiceTest` | Case 4, suggesting a developer who has the skill, team coverage, skill-match percentage |
| `DeliveryEstimateServiceTest` | Capacity up and down, rounding up, no capacity |
| `CostServiceTest` | Working days, hours, cost |
| `UtilizationServiceTest` | Available / High Load / Fully Allocated / Conflict |

### API tests — two ways

**1. In the browser (no extra software).** Sign in as the manager and open
**API Explorer** in the menu, or go straight to
<http://localhost:8080/api.html>.

The page lists all 65 sample requests grouped by area. Pick one, fill in any
`{id}`, edit the JSON body, press **Send**, and read the real status code,
the response time and the response body. It also prints the same call as a
`curl` command you can paste into a terminal.

Requests named **INVALID** or **MISSING** are meant to fail — they
demonstrate the validation rules and the 400/404 handling. This page calls
exactly the same REST API as every other screen; nothing is mocked.

**2. In Postman.** Import
`postman/Project-Resource-Allocation-System.postman_collection.json`. It has
**85 requests in 12 folders**, and each folder contains a successful request,
an invalid request that must return 400, and a missing-record request that
must return 404.

Set the collection variable `baseUrl` to `http://localhost:8080` (it is
already the default) and run the folders top to bottom.

---

## Documentation

| Document | Contents |
|---|---|
| [`implementation_plan.md`](implementation_plan.md) | The plan: architecture, tables, relationships, APIs, rules, phases |
| [`docs/architecture.md`](docs/architecture.md) | Layers, request walk-through, the OpenPDF justification, security note |
| [`docs/database-design.md`](docs/database-design.md) | ER diagram, relationships, normalisation, constraints, indexes |
| [`docs/api-documentation.md`](docs/api-documentation.md) | Every endpoint with request and response examples |
| [`docs/deployment.md`](docs/deployment.md) | How to let an examiner reach the site, and how to password-protect it first |

For the Draw.io diagram, open <https://app.diagrams.net> and redraw the two
ASCII diagrams — the layered architecture in `docs/architecture.md` §2 and
the ER diagram in `docs/database-design.md` §1.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Cannot reach the server` in the browser | The Spring Boot application is not running, or not on port 8080. |
| `Unknown database 'resource_allocation_db'` | Run `database/schema.sql` first. |
| `Access denied for user 'root'` | Correct the username and password in `application.properties`. |
| `Table ... doesn't exist` | Run both SQL scripts. Hibernate is set to `ddl-auto=none` on purpose, so it never creates tables itself — `schema.sql` is the single source of truth. |
| Port 8080 already in use | Change `server.port` in `application.properties`. |
| Pages load but every panel is empty | Load `database/sample-data.sql`. |

---

## Known limitations

These are deliberate, so they can be stated plainly rather than discovered:

* The login is a demo. Passwords are plain text, there is no session token,
  and the REST API itself is not protected — anyone who can reach port 8080
  can call any endpoint. That is fine on your own machine. **Before putting
  the site on the public internet, set `ACCESS_USERNAME` and
  `ACCESS_PASSWORD`**, which puts one shared password in front of the whole
  site. See [`docs/deployment.md`](docs/deployment.md).
* There is no pagination. With a few hundred rows that is fine; with tens of
  thousands the dashboard would slow down.
* Conflict detection runs over all allocations in memory rather than in SQL.
  It is clear and easy to explain, but it is not tuned for a large dataset.
* The cost formula assumes an eight-hour day, a Monday–Friday week and no
  public holidays.
