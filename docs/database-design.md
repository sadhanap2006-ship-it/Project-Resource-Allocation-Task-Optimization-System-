# Database Design

**Database:** `resource_allocation_db` — MySQL 8.0, InnoDB, `utf8mb4`
**Script:** `database/schema.sql` (tables) and `database/sample-data.sql` (demo rows)

---

## 1. Entity-relationship diagram

```
                         ┌──────────────┐
                         │    users     │
                         │──────────────│
                         │ id (PK)      │
                         │ username (U) │
                         │ password     │
                         │ role         │
                         │ developer_id │──────┐  (NULL for a manager)
                         └──────────────┘      │
                                               ▼
   ┌────────────────────┐            ┌────────────────────┐
   │  developer_skills  │            │     developers     │
   │────────────────────│            │────────────────────│
   │ id (PK)            │            │ id (PK)            │
   │ developer_id (FK)  │───────────▶│ name               │
   │ skill_id (FK)      │──┐         │ email (UNIQUE)     │
   │ proficiency_level  │  │         │ role               │
   │ U(developer,skill) │  │         │ hourly_cost        │
   └────────────────────┘  │         │ availability_pct   │
                           │         │ status             │
   ┌────────────────────┐  │         └─────────┬──────────┘
   │      skills        │◀─┘                   │
   │────────────────────│                      │
   │ id (PK)            │◀─┐    ┌──────────────┴──────────────┐
   │ name (UNIQUE)      │  │    │                             │
   │ category           │  │    ▼                             ▼
   └────────────────────┘  │  ┌────────────────────┐   ┌────────────────────┐
                           │  │ developer_leave    │   │ resource_          │
   ┌────────────────────┐  │  │────────────────────│   │   allocations      │
   │  milestone_skills  │  │  │ id (PK)            │   │────────────────────│
   │────────────────────│  │  │ developer_id (FK)  │   │ id (PK)            │
   │ id (PK)            │  │  │ start_date         │   │ developer_id (FK)  │
   │ milestone_id (FK)  │─┐│  │ end_date           │   │ project_id   (FK)  │
   │ skill_id (FK)      │─┘│  │ reason             │   │ milestone_id (FK)  │
   │ U(milestone,skill) │  │  │ status             │   │ start_date         │
   └────────────────────┘  │  └────────────────────┘   │ end_date           │
              │            │                           │ allocation_pct     │
              ▼            │                           └─────────┬──────────┘
   ┌────────────────────┐  │                                     │
   │     milestones     │◀─┼─────────────────────────────────────┘
   │────────────────────│  │
   │ id (PK)            │  │
   │ project_id (FK)    │──┼───┐
   │ name               │  │   │
   │ start_date         │  │   ▼
   │ end_date           │  │ ┌────────────────────┐
   │ progress_pct       │  │ │      projects      │
   │ status             │  │ │────────────────────│
   │ estimated_effort_  │  │ │ id (PK)            │
   │            days    │  │ │ name               │
   └────────────────────┘  │ │ description        │
                           │ │ start_date         │
   ┌────────────────────┐  │ │ end_date           │
   │ simulation_        │  │ │ budget             │
   │      scenarios     │──┼▶│ status             │
   │────────────────────│  │ └─────────┬──────────┘
   │ id (PK)            │  │           │ 1 : 1
   │ project_id (FK)    │  │           ▼
   │ milestone_id (FK)  │──┘ ┌────────────────────┐
   │ current_dev_id(FK) │    │  project_budgets   │
   │ new_dev_id    (FK) │    │────────────────────│
   │ current/simulated  │    │ id (PK)            │
   │   allocation_pct   │    │ project_id (FK, U) │
   │   cost             │    │ total_budget       │
   │   skill_match      │    │ current_spending   │
   │   conflicts        │    └─────────┬──────────┘
   │   delivery_date    │              │ 1 : N
   │ budget_impact      │              ▼
   │ created_at         │    ┌────────────────────┐
   └────────────────────┘    │ budget_            │
                             │    transactions    │
   ┌────────────────────┐    │────────────────────│
   │   notifications    │    │ id (PK)            │
   │────────────────────│    │ project_budget_id  │
   │ id (PK)            │    │ transaction_date   │
   │ type               │    │ amount             │
   │ severity           │    │ category           │
   │ message            │    │ description        │
   │ related_entity_*   │    └────────────────────┘
   │ developer_id (FK)  │
   │ is_read            │      PK = primary key
   │ created_at         │      FK = foreign key
   └────────────────────┘      U  = unique
```

---

## 2. Relationship summary

| Relationship | Type | Notes |
|---|---|---|
| users → developers | N : 1 | A manager account has `developer_id = NULL`. |
| developers ↔ skills | N : M | Through `developer_skills`, which also stores the proficiency level. |
| developers → resource_allocations | 1 : N | The core of the system. |
| developers → developer_leave | 1 : N | Drives the LEAVE CONFLICT rule. |
| projects → milestones | 1 : N | Cascade delete. |
| milestones ↔ skills | N : M | Through `milestone_skills` — the *required* skills. |
| projects → resource_allocations | 1 : N | |
| milestones → resource_allocations | 1 : N | `milestone_id` may be NULL for whole-project work. |
| projects → project_budgets | 1 : 1 | Enforced with a UNIQUE key on `project_id`. |
| project_budgets → budget_transactions | 1 : N | |
| projects / milestones / developers → simulation_scenarios | 1 : N | Only for scenarios the manager chose to save. |
| developers → notifications | 1 : N | `developer_id` is NULL for project-wide alerts. |

---

## 3. Why the two many-to-many links are real tables

`developer_skills` and `milestone_skills` could have been written as JPA
`@ManyToMany`, which hides the join table. They are modelled as ordinary
entities instead because:

1. `developer_skills` carries extra data — `proficiency_level` — which a
   plain `@ManyToMany` cannot store.
2. Both tables can be opened in MySQL Workbench and shown during a viva.
3. The generated SQL is easy to predict, which makes debugging simpler.

---

## 4. Normalisation

The design is in **third normal form**:

* **1NF** — every column holds a single value. Skills are rows in
  `developer_skills`, never a comma-separated string in `developers`.
* **2NF** — every table has a single-column primary key, and every other
  column depends on the whole key. `hourly_cost` belongs to a developer, not
  to an allocation, so it is stored once in `developers`.
* **3NF** — no column depends on another non-key column. A developer's name
  is not copied into `resource_allocations`; the allocation stores only
  `developer_id`.

**The one deliberate exception.** `project_budgets.current_spending` is the
running total of that project's rows in `budget_transactions`, so it is
derived data. It is stored because the dashboard reads it on every page load
and a stored total avoids a `SUM()` each time. `BudgetService.addTransaction()`
is the only place that writes it, so the two can never drift apart.

---

## 5. Constraints used, and what each one protects

| Constraint | Example | Protects against |
|---|---|---|
| PRIMARY KEY | every table | Duplicate or missing identity. |
| FOREIGN KEY | `resource_allocations.developer_id` | An allocation for a developer who does not exist. |
| ON DELETE CASCADE | `milestones.project_id` | Orphan milestones after a project is deleted. |
| ON DELETE SET NULL | `users.developer_id` | Deleting a developer does not delete their login. |
| UNIQUE | `developers.email` | Two developers with the same email. |
| UNIQUE (pair) | `developer_skills(developer_id, skill_id)` | The same skill added twice. |
| NOT NULL | `resource_allocations.allocation_percentage` | A half-filled allocation. |
| CHECK | `allocation_percentage BETWEEN 1 AND 100` | A 250% allocation entered straight into SQL. |
| CHECK | `end_date >= start_date` | Backwards date ranges. |
| CHECK | `status IN (...)` | A typo such as 'IN_PROGRES'. |

The CHECK constraints matter because they hold even if somebody bypasses the
application and runs an `INSERT` by hand. The same rules are also enforced in
Java so the user gets a readable message instead of a database error.

---

## 6. Data types, and why

| Type | Used for | Reason |
|---|---|---|
| `BIGINT AUTO_INCREMENT` | every primary key | Room to grow; MySQL generates the value. |
| `DECIMAL(14,2)` | budgets, costs, amounts | Exact decimal arithmetic. `DOUBLE` would make ₹0.01 rounding errors on money. |
| `DECIMAL(10,2)` | `hourly_cost` | Same reason, smaller range. |
| `INT` | percentages, effort days | Whole numbers only. |
| `DATE` | all project and leave dates | The time of day is irrelevant here. |
| `DATETIME` | `created_at` | A notification needs the time as well as the date. |
| `VARCHAR(n) + CHECK` | statuses and enums | Maps cleanly to a Java `enum` with `@Enumerated(EnumType.STRING)`, and the value is readable in the table. |
| `BOOLEAN` | `is_read` | Stored by MySQL as `TINYINT(1)`. |

**Why `VARCHAR` + `CHECK` instead of MySQL's `ENUM` type?** A MySQL `ENUM`
needs an `ALTER TABLE` to add a value, and it maps less predictably to JPA.
`VARCHAR` with a `CHECK` gives the same protection, works with
`@Enumerated(EnumType.STRING)` and is easier to read.

---

## 7. Indexes

Beyond the automatic primary-key and unique indexes:

| Index | Table | Why |
|---|---|---|
| `idx_alloc_developer` | resource_allocations | Conflict detection loads every allocation of one developer. |
| `idx_alloc_project` | resource_allocations | The project page lists a project's allocations. |
| `idx_alloc_milestone` | resource_allocations | Skill-gap detection and the delivery estimate run per milestone. |
| `idx_ms_project` | milestones | Timeline and project pages. |
| `idx_leave_developer` | developer_leave | Leave-conflict detection. |
| `idx_txn_budget` | budget_transactions | Budget page and burn-rate calculation. |
| `idx_notif_developer` | notifications | The developer dashboard shows only that person's alerts. |

Every one of these matches a `findBy...` method on a repository.

---

## 8. What the sample data demonstrates

`database/sample-data.sql` is built so the dashboard is useful the moment
the application starts:

| # | Demonstrates | Where to see it |
|---|---|---|
| A | **Over-allocation conflict** — Rahul at 60% + 50% on overlapping dates = 110% | Conflicts page, red row |
| B | **Skill gap with nobody available** — "Cloud Deployment" needs Azure and no developer has it | Conflicts page, skill gaps |
| C | **Skill gap with a suggestion** — "Data Migration" needs Java and MySQL, nobody allocated yet | Conflicts page |
| D | **Leave conflict** — Priya is 80% allocated during her 10–14 Sep leave | Conflicts page, amber row |
| E | **Budget warning** — E-Commerce project is 90% spent → CRITICAL | Budget page, red row |
| F | **Budget forecast** — Hospital project: ₹500,000 / ₹300,000 spent / ₹10,000 per day → "approximately 20 days" | Budget page, forecast |
| G | **No spending data** — College ERP has no transactions, so no division happens | Budget page, third row |
| H | **A clean allocation** — Arun at 60% with every required skill and no conflict | Allocations page |
| I | **Non-overlapping work is not a conflict** — Divya works in September and again in late October | Conflicts page (she does not appear) |
