# ARCHITECTURE DOCUMENTATION: DFD & ER SPECIFICATIONS

## SIST CSE AI Capstone Project

---

## 1. Data Flow Diagrams (DFD)

### 1.1 DFD Level 0 (Context Diagram)

```text
               +-------------------------------------------------------+
               |                                                       |
               |                Project Manager                        |
               |                                                       |
               +---------------------------+---------------------------+
                                           |
                                           | Credentials, Projects, Allocations,
                                           | Leave approvals, Simulation requests
                                           v
               +-------------------------------------------------------+
               |                                                       |
               |                         [ 0 ]                         |
               |           Project Resource Allocation System          |
               |                                                       |
               +---------------------------+---------------------------+
                                           ^
                                           | Credentials, Leave requests,
                                           | Workload views
                                           |
               +---------------------------+---------------------------+
               |                                                       |
               |                     Developer                         |
               |                                                       |
               +-------------------------------------------------------+
                                           |
                                           | Read / Write Queries
                                           v
               +-------------------------------------------------------+
               | [D1]               MySQL 8.0 Database                 |
               +-------------------------------------------------------+
```

### 1.2 DFD Level 1 (Decomposed Subsystems)

```text
                                  +-------------------+
                                  |  Project Manager  |
                                  +---------+---------+
                                            |
         +------------------+---------------+-------------------+------------------+
         | 1.0              | 2.0           | 3.0               | 4.0              | 5.0
         v                  v               v                   v                  v
+-----------------+ +---------------+ +-----------------+ +-----------------+ +-----------------+
| Authentication  | | Project &     | | Resource        | | Conflict & Skill| | What-If Swap    |
| & Role Dispatch | | Milestone Mgmt| | Allocation Mgmt | | Gap Detection   | | Simulator       |
+--------+--------+ +-------+-------+ +--------+--------+ +--------+--------+ +--------+--------+
         |                  |                  |                   |                   |
         +------------------+------------------+-------------------+-------------------+
                                            |
                                            v (JDBC SQL)
                            +-------------------------------+
                            | [D1]     MySQL Database       |
                            |   - users                     |
                            |   - developers                |
                            |   - projects & milestones     |
                            |   - resource_allocations      |
                            |   - developer_availability    |
                            |   - budgets                   |
                            |   - notifications             |
                            |   - what_if_scenarios         |
                            +-------------------------------+
```

---

## 2. Entity-Relationship (ER) Diagram Specification

### 2.1 Entities, Keys, and Attributes

1. **`users`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `name` (VARCHAR)
   - `email` (VARCHAR, UNIQUE)
   - `password` (VARCHAR)
   - `role` (VARCHAR)

2. **`developers`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `user_id` (FK -> `users.id`, NULLABLE)
   - `name` (VARCHAR)
   - `email` (VARCHAR, UNIQUE)
   - `role_title` (VARCHAR)
   - `hourly_cost` (DECIMAL)
   - `availability_percentage` (INT)
   - `status` (VARCHAR)

3. **`skills`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `name` (VARCHAR, UNIQUE)
   - `category` (VARCHAR)

4. **`developer_skills`** (Junction Table)
   - `developer_id` (PK, FK -> `developers.id`)
   - `skill_id` (PK, FK -> `skills.id`)

5. **`projects`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `name` (VARCHAR)
   - `description` (TEXT)
   - `start_date` (DATE)
   - `end_date` (DATE)
   - `total_budget` (DECIMAL)
   - `current_spend` (DECIMAL)
   - `status` (VARCHAR)

6. **`milestones`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `project_id` (FK -> `projects.id`)
   - `name` (VARCHAR)
   - `description` (TEXT)
   - `start_date` (DATE)
   - `end_date` (DATE)
   - `estimated_effort_hours` (DECIMAL)
   - `progress_percentage` (INT)
   - `status` (VARCHAR)

7. **`milestone_skills`** (Junction Table)
   - `milestone_id` (PK, FK -> `milestones.id`)
   - `skill_id` (PK, FK -> `skills.id`)

8. **`resource_allocations`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `developer_id` (FK -> `developers.id`)
   - `project_id` (FK -> `projects.id`)
   - `milestone_id` (FK -> `milestones.id`)
   - `allocation_percentage` (INT)
   - `start_date` (DATE)
   - `end_date` (DATE)
   - `hourly_cost_snapshot` (DECIMAL)

9. **`developer_availability`**
   - `id` (PK, INT, AUTO_INCREMENT)
   - `developer_id` (FK -> `developers.id`)
   - `start_date` (DATE)
   - `end_date` (DATE)
   - `reason` (VARCHAR)
   - `status` (VARCHAR)

10. **`budgets`**
    - `id` (PK, INT, AUTO_INCREMENT)
    - `project_id` (FK -> `projects.id`, UNIQUE)
    - `total_budget` (DECIMAL)
    - `current_spend` (DECIMAL)

11. **`notifications`**
    - `id` (PK, INT, AUTO_INCREMENT)
    - `user_id` (FK -> `users.id`)
    - `title` (VARCHAR)
    - `message` (TEXT)
    - `type` (VARCHAR)
    - `severity` (VARCHAR)
    - `is_read` (BOOLEAN)

---

## 3. Draw.io Reproduction Guidelines for Viva

To reproduce these diagrams for your project thesis and presentation slides:

1. Open [Draw.io](https://app.diagrams.net/).
2. Select **Blank Diagram**.
3. **For DFDs**:
   - External entities: Use **Process / Rectangle** shapes (`Actor` or `Box`).
   - Processes: Use **Circles** or **Rounded Rectangles** with numbering (`[0]`, `[1.0]`, etc.).
   - Data Stores: Use open-ended parallel line rectangles (`Data Store`).
   - Arrows: Labeled unidirectional connectors representing data packets.
4. **For ER Diagrams**:
   - Enable the **Entity Relation** shape library (left panel -> More Shapes -> Entity Relation).
   - Drop `Table` blocks for each entity.
   - Primary Keys marked with bold underline `PK`.
   - Foreign keys marked with `FK`.
   - Connect relationships using crow's foot notation (1:1, 1:N, M:N).
