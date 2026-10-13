# Project Resource Allocation System

> **SIST CSE AI Capstone Project — Master Engineering Implementation**  
> Complete Full-Stack Web Application built with **Pure Java SE (com.sun.net.httpserver)**, **Vanilla HTML5/CSS3/ES6+ JavaScript**, and **MySQL 8.0 (3NF Normalized)**.

---

## 1. Project Overview

In software development organizations, ineffective resource planning leads to costly project delays, developer burnout from over-allocation (>100%), delivery risks from unplanned leaves, and milestone skill gaps.

The **Project Resource Allocation System** is an enterprise-grade resource planning and risk mitigation platform. It provides project managers with real-time visibility into developer bandwidth, automated conflict detection algorithms, dynamic budget burn forecasting, and an in-memory "What-If" scenario simulation engine that evaluates staffing adjustments with zero risk to the production database.

### Key Highlights
- **100% Framework-Free Backend**: Built with standard Java SE and built-in `HttpServer`. No Spring Boot, no Hibernate, no Maven/Gradle.
- **Pure JDBC Data Access**: Robust SQL parameterized `PreparedStatement` queries adhering strictly to 3NF normalization.
- **Modern Responsive UI**: Pure CSS Grid & Flexbox, Inter typography, dark slate theme with glassmorphism touches, and Chart.js for analytics.
- **Academic Viva Ready**: Includes 3 instant one-click scenario presets in the header for evaluators.

---

## 2. Technology Stack & Justification

| Layer | Technology | Justification |
|---|---|---|
| **Backend Runtime** | Java SE 17+ | Core language with strong type safety, memory safety, and cross-platform execution. |
| **HTTP Server** | `com.sun.net.httpserver.HttpServer` | Built-in Java standard library server. Zero third-party dependencies, instant startup (<1 sec), and clear RFC HTTP handling. |
| **Persistence / Data** | MySQL 8.0 & Pure JDBC | High-performance relational database with 13 tables normalized to 3NF. `PreparedStatement` prevents SQL injection. |
| **Database Driver** | `mysql-connector-j-8.3.0.jar` | Official Type 4 JDBC driver located in `backend/lib/`. |
| **Frontend Structure** | HTML5 Semantic Elements | Accessible, well-structured single-page application with descriptive element IDs. |
| **Frontend Styling** | Vanilla CSS3 (Grid & Flexbox) | High-performance responsive layouts (360px to 1920px), customized color tokens, toast animations. |
| **Frontend Client** | Vanilla ES6+ JavaScript | Modern asynchronous `fetch()` API, client-side routing, live capacity calculator. |
| **Visual Analytics** | Chart.js 4.4.1 (via CDN) | Lightweight HTML5 canvas library strictly used for project burn-down charts. |

---

## 3. System Architecture & Repository Structure

```
project-resource-allocation-system/
├── backend/
│   ├── lib/
│   │   └── mysql-connector-j-8.3.0.jar   # Official MySQL Type-4 JDBC Driver
│   └── src/
│       ├── Main.java                      # HttpServer bootstrap & static file router
│       ├── controller/                    # BaseController & HTTP endpoint handlers
│       │   ├── AuthController.java
│       │   ├── DeveloperController.java
│       │   ├── SkillController.java
│       │   ├── ProjectController.java
│       │   ├── MilestoneController.java
│       │   ├── AllocationController.java
│       │   ├── AvailabilityController.java
│       │   ├── DashboardController.java
│       │   ├── NotificationController.java
│       │   └── SimulationController.java
│       ├── dao/                           # Pure JDBC PreparedStatement DAOs
│       │   ├── UserDAO.java
│       │   ├── DeveloperDAO.java
│       │   ├── SkillDAO.java
│       │   ├── ProjectDAO.java
│       │   ├── MilestoneDAO.java
│       │   ├── AllocationDAO.java
│       │   ├── AvailabilityDAO.java
│       │   ├── NotificationDAO.java
│       │   └── SimulationDAO.java
│       ├── model/                         # Pure Java domain POJOs
│       │   ├── User.java
│       │   ├── Developer.java
│       │   ├── Skill.java
│       │   ├── Project.java
│       │   ├── Milestone.java
│       │   ├── Allocation.java
│       │   ├── Availability.java
│       │   ├── Notification.java
│       │   └── WhatIfScenario.java
│       ├── service/                       # Business logic & algorithmic services
│       │   ├── AuthService.java
│       │   ├── ConflictDetectionService.java # Daily sweep-line boundary algorithm
│       │   ├── SkillGapService.java
│       │   ├── BudgetService.java
│       │   ├── SimulationService.java     # In-memory swap simulation
│       │   ├── AllocationService.java
│       │   └── NotificationService.java
│       └── util/                          # JSON parser, DBConnection, Validation
│           ├── DBConnection.java
│           ├── JsonUtil.java
│           └── ValidationUtil.java
├── database/
│   ├── schema.sql                         # 13 tables, foreign keys, indexes (3NF)
│   └── sample_data.sql                    # Seed data with deliberate test conflicts
├── docs/
│   ├── DEFINE.md                          # Personas, user stories, acceptance criteria
│   ├── DESIGN.md                          # 3-tier architecture, data flow, validation
│   ├── architecture.md                    # DFD Level 0/1, ER diagrams, Draw.io guide
│   ├── API.md                             # Complete REST API specification
│   ├── ACCEPTANCE_TESTING.md              # Test execution matrix (AC-01 to AC-10)
│   ├── POSTMAN_TESTING.md                 # cURL commands and Postman setup
│   └── VIVA_GUIDE.md                      # 16 standard viva questions and answers
├── frontend/
│   ├── css/
│   │   └── style.css                      # Pure CSS Grid & Flexbox, dark theme
│   ├── js/
│   │   └── script.js                      # Vanilla JavaScript SPA logic & fetch API
│   └── index.html                         # Comprehensive single-page web application
├── .gitignore
├── run.bat                                # 1-click Windows compilation & launch script
└── README.md
```

---

## 4. Key Features & Business Capabilities

1. **Manager Executive Dashboard**:
   - Live KPI cards: Active Projects, Team Headcount, Average Developer Utilization %, Total Budget & Spend, Critical Conflict Alerts.
   - Project health summaries with progress bars and budget status.
2. **Resource Allocation & Live Capacity Calculator**:
   - Visual allocation percentage slider with real-time feedback: Green ($\le 80\%$), Amber ($81\%-100\%$), Crimson ($> 100\%$).
   - Monthly team allocation heatmap showing density across calendar months.
3. **Automated Real-Time Conflict Detection**:
   - **Over-Allocation (>100%)**: Detects overlapping date ranges where cumulative allocation exceeds full-time capacity.
   - **Leave Conflicts**: Intersects milestone commitments with approved developer leave.
   - **Skill Gap Alerts**: Flags when a developer lacks required milestone competencies.
4. **"What-If" Scenario Simulator**:
   - In-memory modeling of developer reassignments.
   - Evaluates cost variance ($\Delta \text{Cost}$), delivery impacts ($\Delta \text{Days}$), and potential resulting conflicts *without touching the database*.
   - One-click "Apply Swap" safely commits the approved change to the database.
5. **Restricted Developer Portal**:
   - Personalized view for individual engineers showing assigned milestones, current workload, remaining capacity, and personal notifications.
6. **Academic Viva Evaluation Helper**:
   - Top banner with 3 instant preset scenarios to immediately demonstrate conflict detection to external examiners.

---

## 5. Demo Credentials

| Role | Email | Password | Access Scope |
|---|---|---|---|
| **Project Manager** | `manager@company.com` | `admin123` | Full administrative control, all projects, what-if simulator, conflicts. |
| **Developer (Arun Kumar)** | `arun@company.com` | `dev123` | Restricted personal portal, assigned milestones, remaining capacity. |

---

## 6. Installation & Execution Guide

### Prerequisites
1. **Java Development Kit (JDK 17 or higher)** installed and available in your system `PATH`.
2. **MySQL Server 8.0** running locally on port 3306 with user `root` and password `root`.
   *(Note: If your local credentials differ, edit `backend/src/util/DBConnection.java`).*

### Step 1: Database Setup
Open PowerShell or MySQL Command Line Client:
```powershell
mysql -u root -proot -e "DROP DATABASE IF EXISTS resource_allocation; CREATE DATABASE resource_allocation;"
mysql -u root -proot resource_allocation < database/schema.sql
mysql -u root -proot resource_allocation < database/sample_data.sql
```

### Step 2: One-Click Execution (Windows)
Double-click `run.bat` or execute in PowerShell:
```cmd
run.bat
```
This batch file will:
1. Compile all Java sources in `backend/src/` into `backend/bin/`.
2. Automatically launch your default browser at `http://localhost:8080/index.html`.
3. Start the Java HTTP Server on port 8080.

---

## 7. Viva & Demonstration Walkthrough

When presenting to evaluators, follow this recommended sequence:
1. **Explain the 3-Tier Architecture**: Reference `docs/architecture.md` and show the pure Java SE backend without framework dependencies.
2. **Demonstrate Conflict Detection**: Click the top header toggle **"Academic Viva & Defense Guide"**:
   - Click **Scenario A**: Shows Priya Sharma's 110% over-allocation across Project Alpha & Beta.
   - Click **Scenario B**: Shows Rahul Verma's approved medical leave clash on Milestone 2.
   - Click **Scenario C**: Shows Arun Kumar's missing Python skill on Milestone 4.
3. **Demonstrate What-If Simulator**: Navigate to **What-If Simulator**, select Milestone 1, swap Priya Sharma with Arun Kumar, and click "Run In-Memory Simulation". Point out that Arun's lower rate saves ₹16,000, but causes a critical over-allocation conflict, and the database was *not* modified.
4. **Demonstrate Developer Portal**: Use the persona switcher in the top right to switch to **Arun Kumar (Developer)** to show restricted RBAC in action.

---

## 8. License & Academic Attribution
Developed as part of the **SIST CSE AI Capstone Project** curriculum. Designed for academic demonstration, defense, and evaluation.
