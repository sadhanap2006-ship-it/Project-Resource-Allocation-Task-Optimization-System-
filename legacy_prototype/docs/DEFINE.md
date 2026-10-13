# DEFINE PHASE: Project Resource Allocation System

## SIST CSE AI Capstone Project Specification

---

## 1. Project Overview & Scope

### 1.1 Problem Statement
IT project managers face constant challenges when balancing human engineering capacity, specialist skills, and project milestones across multiple overlapping projects. In the absence of an automated, centralized resource intelligence system, organizations face developer over-allocation, burnout, critical skill shortages, unmanaged leave overlap, and undetected budget burn-rates.

### 1.2 In Scope
1. **Resource Profiles & Skills Matrix**: Centralized repository of developer records with hourly rates, maximum availability percentages, and relational skill mappings.
2. **Project & Milestone Scheduling**: Multi-project tracking with deliverables, date bounds, effort estimates, and skill requirements.
3. **Resource Allocation**: Milestone-level developer allocation with percentage assignments.
4. **Automated Over-Allocation Conflict Engine**: Sweep-line algorithm calculating peak cumulative allocation across overlapping dates, triggering warnings (>100%).
5. **Skill-Gap Analysis**: Milestone required skill verification against assignee skill sets.
6. **Availability & Leave Management**: Calendar tracking of planned developer vacation and automatic leave conflict detection.
7. **Budget Monitoring & Burn-Rate Forecasting**: Daily spending rate calculation and projected budget exhaustion dates.
8. **Interactive Visualizations**: Project timeline and color-coded weekly developer utilization heatmap.
9. **What-If Resource Swap Simulator**: Isolated in-memory swap simulation with delivery impact, cost variance, and skill match assessment without database mutation.
10. **Report Export**: Browser-native print and PDF generation for simulated scenarios.
11. **In-App Notification Drawer**: System-generated alerts stored in MySQL for conflicts and risk events.
12. **Role-Based Views**: Dedicated Project Manager administrative dashboard and developer-specific portal.

### 1.3 Out of Scope
- External SMTP email sending or SMS gateways (in-app notifications are sufficient for capstone evaluation).
- Slack, Microsoft Teams, or Jira third-party webhook integrations.
- Multi-region cloud compute provisioning.
- Enterprise SSO / OAuth2 / LDAP (simple database-backed authentication is implemented).
- Advanced black-box deep learning models (all logic is deterministic, transparent, and viva-explainable).

---

## 2. Stakeholder Personas

### Persona 1: Rajesh Ramanathan (Project Manager)
- **Role**: Lead Technical Project Manager overseeing 4 enterprise software streams.
- **Goals**: Deliver milestones on schedule, prevent developer over-allocation, balance budgets, and evaluate resource swap decisions before committing them.
- **Pain Points**: Spreadsheets are out-of-date; unable to immediately see if assigning a developer to Module B creates an unworkable 120% load or violates an upcoming planned vacation.
- **System Needs**: Single-pane dashboard, real-time conflict badges, budget burn forecasting, and a risk-free What-If simulator.

### Persona 2: Arun Kumar (Full-Stack Developer)
- **Role**: Senior Java & Cloud Developer.
- **Goals**: Clear visibility of milestone expectations, fair workload distribution (<= 100%), and seamless leave logging.
- **Pain Points**: Double-booked across concurrent sprints without prior notice; difficulty finding out which project milestones depend on his skills.
- **System Needs**: Developer portal showing personal project assignments, weekly workload percentage, and notifications of schedule adjustments.

---

## 3. User Stories

1. **US-01: Resource Allocation**
   - *As a* Project Manager,
   - *I want to* assign a developer to a specific project milestone with a designated allocation percentage,
   - *So that* team workload and effort hours are properly tracked against deliverables.

2. **US-02: Over-Allocation Conflict Detection**
   - *As a* Project Manager,
   - *I want* the system to automatically detect when a developer's cumulative allocation exceeds 100% across concurrent active milestones,
   - *So that* I can prevent developer burnout and schedule slippage.

3. **US-03: Skill-Gap Alerting**
   - *As a* Project Manager,
   - *I want to* be alerted if a milestone requires technical skills not possessed by the assigned developer,
   - *So that* I can reassign the milestone to a qualified engineer or arrange training.

4. **US-04: Availability & Leave Conflict Tracking**
   - *As a* Developer or Project Manager,
   - *I want to* record planned leaves and have the system flag allocations during those dates,
   - *So that* project schedules account for real developer availability.

5. **US-05: Budget Burn-Rate Forecasting**
   - *As a* Project Manager,
   - *I want to* view current spend, daily burn rate, and a projected budget exhaustion date,
   - *So that* I can take corrective action before a project experiences cost overruns.

6. **US-06: What-If Resource Swap Simulation**
   - *As a* Project Manager,
   - *I want to* simulate swapping Developer A with Developer B on a milestone without modifying the database,
   - *So that* I can review the cost difference, delivery date shift, and conflict impact before committing the change.

---

## 4. Acceptance Criteria (Given / When / Then)

### AC-01: Authentication & Role Dashboards
- **Given** valid credentials for a Project Manager (`manager@company.com` / `admin123`),
- **When** the user logs into the system,
- **Then** the system displays the comprehensive Project Manager Dashboard with KPI cards, timeline, budget metrics, and management tabs.
- **Given** valid credentials for a Developer (`arun@company.com` / `dev123`),
- **When** the developer logs into the system,
- **Then** the system displays the Developer Portal showing personal allocations, weekly workload, and personal leaves, with administrative controls hidden.

### AC-02: Over-Allocation Conflict Detection
- **Given** Developer Priya is assigned 60% to Project Alpha (Oct 1 – Oct 20),
- **When** the Project Manager allocates Priya 50% to Project Beta (Oct 10 – Oct 25),
- **Then** the conflict engine detects a peak overlap of 110% (>100%), displays an `OVER-ALLOCATION CONFLICT` badge, and persists a warning notification.

### AC-03: Skill-Gap Alerting
- **Given** Milestone "Cloud Infrastructure Setup" requires the skill `AWS`,
- **When** the Project Manager allocates Developer Arun who only possesses `Java` and `MySQL`,
- **Then** the system flags a `SKILL GAP ALERT` displaying `Missing Skill: AWS` and highlights the mismatch on the dashboard.

### AC-04: Leave Conflict Detection
- **Given** Developer Rahul has an approved leave from Oct 12 to Oct 16,
- **When** Rahul is allocated to a milestone running from Oct 10 to Oct 20,
- **Then** the system detects a `LEAVE CONFLICT` indicating 0% effective capacity during the leave period.

### AC-05: Budget Burn Rate & Exhaustion Forecast
- **Given** a project with total budget ₹5,00,000, current spend ₹3,50,000, and elapsed duration of 20 days,
- **When** the Project Manager views the budget monitoring section,
- **Then** the system outputs a daily burn rate of ₹17,500/day and forecasts exhaustion in approximately 8.5 days.

### AC-06: What-If Resource Swap Simulator
- **Given** a planned milestone currently assigned to Developer A (₹600/hr),
- **When** the Project Manager selects proposed Developer B (₹900/hr) and clicks `SIMULATE`,
- **Then** the system renders a side-by-side comparison showing cost difference, delivery date delta, and conflict status without altering real database allocations.
- **When** the Project Manager clicks `APPLY CHANGE`,
- **Then** the system updates the real allocation record in MySQL and recalculates system conflicts.
