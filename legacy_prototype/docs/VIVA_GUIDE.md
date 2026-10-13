# Academic Viva & Defense Guide (16 Questions & Answers)

## SIST CSE AI Capstone Project — Project Resource Allocation System

This document equips 3rd-year CSE students with concise, technically sound answers to expected viva and project defense questions.

---

### Q1: What is the core problem solved by your Project Resource Allocation System?
**Answer:** In software enterprises, project managers frequently struggle with developer over-allocation (>100% capacity), unplanned leave clashes, milestone skill mismatches, and unexpected budget run-out. Our system provides an automated, real-time conflict detection engine, predictive budget forecasting, and an in-memory "What-If" scenario simulator that allows managers to model staffing decisions before committing them to the database.

---

### Q2: Why did you implement the system using Pure Java SE and built-in HttpServer instead of Spring Boot or Hibernate?
**Answer:** Implementing with pure Java SE and `com.sun.net.httpserver.HttpServer` demonstrates a foundational understanding of computer science concepts:
1. Direct handling of HTTP network exchanges, RFC methods, headers, status codes, and JSON serialization.
2. Direct connection lifecycle and transactional control using pure JDBC `PreparedStatement` without the hidden overhead or "magic" of ORM reflection.
3. Zero external framework dependencies, making the project lightweight, portable, fast-starting (<1 second), and fully transparent during evaluation.

---

### Q3: How is the database schema structured to ensure 3NF normalization?
**Answer:** The schema consists of 13 tables separated cleanly into entities and junction tables:
- **1NF**: All table attributes are atomic with defined primary keys (`id`).
- **2NF**: No partial functional dependencies; all non-key columns depend on the entire primary key (junction tables like `developer_skills`, `project_skills`, and `milestone_skills` use composite primary keys).
- **3NF**: No transitive dependencies. For example, project budgets and spend calculations are decoupled into `budgets`, developer leave records into `developer_availability`, and snapshots of hourly costs are captured at allocation time (`hourly_cost_snapshot`) to preserve historical integrity.

---

### Q4: How does the Conflict Detection Engine detect over-allocation?
**Answer:** The system uses a daily sweep-line boundary evaluation algorithm. When querying allocations for a developer:
1. It gathers all date boundaries across active assignments.
2. For each active calendar date, it sums the `allocation_percentage` across overlapping milestones.
3. If the cumulative sum exceeds 100%, an `OVER_ALLOCATION_CONFLICT` is triggered, flagging the exact peak percentage (e.g., Priya Sharma at 110%) and the conflicting milestone pairs.

---

### Q5: How are Leave Conflicts detected?
**Answer:** The engine cross-references the developer's assigned milestones (`start_date` to `end_date`) against approved leave periods in the `developer_availability` table where `status = 'APPROVED'`. If `(leave.start_date <= milestone.end_date) AND (leave.end_date >= milestone.start_date)`, an overlap exists. The developer's effective capacity during those dates drops to 0%, triggering a `LEAVE_CONFLICT` warning.

---

### Q6: How does the Skill Gap Detection mechanism work?
**Answer:** The `SkillGapService` retrieves the set of required skill IDs for a milestone from `milestone_skills` and compares it with the assigned developer's competencies from `developer_skills`. If `requiredSkills \ developerSkills` is non-empty, the difference represents missing skills, triggering a `SKILL_GAP_ALERT` with targeted training or reassignment recommendations.

---

### Q7: What is the "What-If" Simulation and how does it prevent accidental database corruption?
**Answer:** The What-If Simulator executes in-memory. It loads active milestone details and developer profiles, simulates swapping Developer A with Developer B, calculates cost deltas, delivery impact, and projected capacity spikes *without issuing any SQL `UPDATE` or `INSERT` to the allocation table*. The manager can review the verdict (`VIABLE`, `OVER_BUDGET`, or `CRITICAL_RISK`). Only when the manager explicitly clicks "Apply Swap" is a database transaction executed.

---

### Q8: How is the Project Budget burn rate and exhaustion forecast calculated?
**Answer:** 
$$\text{Elapsed Days} = \text{Current Date} - \text{Project Start Date}$$
$$\text{Daily Burn Rate} = \frac{\text{Current Spend}}{\text{Elapsed Days}}$$
$$\text{Days to Exhaustion} = \frac{\text{Total Budget} - \text{Current Spend}}{\text{Daily Burn Rate}}$$
If the projected exhaustion date falls before the project end date, an alert is dispatched indicating a high risk of budget overrun.

---

### Q9: How does the system handle SQL Injection vulnerabilities?
**Answer:** Every single database query across all Data Access Objects (`UserDAO`, `DeveloperDAO`, `ProjectDAO`, `AllocationDAO`, etc.) strictly uses parameterized `PreparedStatement`. User inputs are treated strictly as data parameters rather than executable SQL syntax, completely eliminating SQL injection risks.

---

### Q10: How does your frontend communicate with the backend?
**Answer:** The frontend is built with vanilla HTML5, CSS3, and modern ES6+ JavaScript using the native `fetch()` API. It asynchronously sends HTTP requests (`GET`, `POST`, `PUT`, `DELETE`) with `application/json` headers to `http://localhost:8080/api/*`, dynamically rendering responses into the DOM without page refreshes.

---

### Q11: Why did you use Chart.js for visualizations?
**Answer:** Chart.js is an industry-standard HTML5 canvas charting library. We use it strictly for responsive, high-performance visual analytics:
1. Visualizing project budget utilization vs. remaining balance.
2. Rendering allocation load distribution across the engineering team.
All business logic, aggregation, and filtering are performed on the Java backend, while Chart.js handles pure visual presentation.

---

### Q12: How is role-based access control (RBAC) enforced?
**Answer:** The application defines two distinct personas:
1. **Project Manager (`PROJECT_MANAGER`)**: Full access to global KPIs, project creation, developer allocation, conflict resolution, what-if simulations, and team-wide heatmaps.
2. **Developer (`DEVELOPER`)**: Restricted access to their personal dashboard, showing only their assigned milestones, personal workload percentage, remaining capacity, and personal notifications.

---

### Q13: What happens when an allocation exceeds 100% in the UI?
**Answer:** The Assign Resource modal features a live capacity calculator. As the manager adjusts the allocation percentage slider:
- Load $\le 80\%$: Progress bar displays Emerald Green (`#10b981`).
- Load $81\% - 100\%$: Progress bar turns Amber (`#f59e0b`).
- Load $> 100\%$: Progress bar shifts to Crimson Red (`#ef4444`) with a bold warning notice alerting the manager before submission.

---

### Q14: How does your backend handle concurrency and multi-threaded requests?
**Answer:** The built-in `HttpServer` is configured with a fixed thread pool (`Executors.newFixedThreadPool(12)`). Each incoming client HTTP exchange is dispatched to a worker thread from the pool. Database connections are opened and closed cleanly per operation using try-with-resources, preventing connection leaks.

---

### Q15: How can an evaluator quickly verify the conflict detection scenarios?
**Answer:** In the top header of the web application, clicking the **"Academic Viva & Defense Guide"** toggle opens three one-click scenarios:
- **Scenario A**: Highlights Priya Sharma's 110% over-allocation across Project Alpha & Beta.
- **Scenario B**: Highlights Rahul Verma's approved medical leave conflict on Milestone 2.
- **Scenario C**: Highlights Arun Kumar's missing Python skill on Milestone 4.
Clicking any scenario instantly switches to the Conflict Center and highlights the relevant finding.

---

### Q16: What future enhancements could be added to this project?
**Answer:** Potential future extensions include:
1. Machine learning algorithms (e.g., genetic algorithms or integer linear programming) to auto-recommend optimal resource schedules.
2. Integration with corporate calendar APIs (Google Calendar / Outlook) for automated leave synchronization.
3. WebSocket support for live multi-manager collaboration updates.
