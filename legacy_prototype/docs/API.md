# API CONTRACT DOCUMENTATION

## SIST CSE AI Capstone Project — REST API Specifications

All endpoints return a standardized JSON envelope:
```json
{
  "status": "SUCCESS",   // or "ERROR"
  "message": "Human readable status description",
  "data": { ... }        // JSON payload or null on error
}
```

---

## 1. Authentication

### `POST /api/login`
- **Purpose**: Authenticate user and return role metadata.
- **Request Body**:
  ```json
  {
    "email": "manager@company.com",
    "password": "admin123"
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "status": "SUCCESS",
    "message": "Login successful",
    "data": {
      "userId": 1,
      "name": "Rajesh Ramanathan",
      "email": "manager@company.com",
      "role": "PROJECT_MANAGER",
      "developerId": null
    }
  }
  ```
- **Error Response (401 Unauthorized)**:
  ```json
  {
    "status": "ERROR",
    "message": "Invalid email or password",
    "data": null
  }
  ```

---

## 2. Developers

### `GET /api/developers`
- **Purpose**: List all developer resource profiles with active allocations and remaining capacity.
- **Success Response (200 OK)**:
  ```json
  {
    "status": "SUCCESS",
    "message": "Developers retrieved",
    "data": [
      {
        "id": 1,
        "name": "Arun Kumar",
        "email": "arun@company.com",
        "roleTitle": "Backend Java Developer",
        "department": "Engineering",
        "hourlyCost": 600.00,
        "availabilityPercentage": 100,
        "currentAllocation": 70,
        "remainingCapacity": 30,
        "status": "ACTIVE",
        "skills": ["Java", "MySQL", "REST API"]
      }
    ]
  }
  ```

### `GET /api/developers/{id}`
- **Purpose**: Retrieve single developer details.

### `POST /api/developers`
- **Purpose**: Register a new developer.
- **Request Body**:
  ```json
  {
    "name": "Ananya Roy",
    "email": "ananya@company.com",
    "roleTitle": "Cloud DevOps Engineer",
    "department": "Infrastructure",
    "hourlyCost": 850.00,
    "availabilityPercentage": 100,
    "skillIds": [5, 6]
  }
  ```

### `PUT /api/developers/{id}`
- **Purpose**: Update existing developer profile.

---

## 3. Skills

### `GET /api/skills`
- **Purpose**: List all available skills.

### `POST /api/skills`
- **Purpose**: Add a new skill.

---

## 4. Projects

### `GET /api/projects`
- **Purpose**: List all projects with status, budget utilization, and milestone counts.

### `GET /api/projects/{id}`
- **Purpose**: Get project by ID.

### `POST /api/projects`
- **Purpose**: Create a new project.
- **Request Body**:
  ```json
  {
    "name": "Project Epsilon",
    "description": "Next-gen authentication service",
    "startDate": "2026-11-01",
    "endDate": "2027-03-31",
    "totalBudget": 400000.00,
    "status": "PLANNED"
  }
  ```

### `PUT /api/projects/{id}`
- **Purpose**: Update project.

### `DELETE /api/projects/{id}`
- **Purpose**: Remove project.

---

## 5. Milestones

### `GET /api/milestones`
- **Purpose**: List all milestones across projects.

### `POST /api/milestones`
- **Purpose**: Create a milestone for a project.

### `PUT /api/milestones/{id}`
- **Purpose**: Update milestone progress or dates.

### `DELETE /api/milestones/{id}`
- **Purpose**: Delete milestone.

---

## 6. Resource Allocations

### `GET /api/allocations`
- **Purpose**: List all active allocations.

### `POST /api/allocations`
- **Purpose**: Assign developer to milestone.
- **Request Body**:
  ```json
  {
    "developerId": 1,
    "projectId": 1,
    "milestoneId": 1,
    "allocationPercentage": 50,
    "startDate": "2026-10-01",
    "endDate": "2026-10-20"
  }
  ```

### `DELETE /api/allocations/{id}`
- **Purpose**: Remove resource allocation.

---

## 7. Availability & Leave

### `GET /api/availability`
- **Purpose**: Retrieve planned leaves and availability records.

### `POST /api/availability`
- **Purpose**: Schedule planned leave for a developer.

### `DELETE /api/availability/{id}`
- **Purpose**: Cancel planned leave.

---

## 8. Conflicts & Skill Gaps

### `GET /api/conflicts`
- **Purpose**: Evaluate and list all system conflicts (over-allocations, leave overlaps).
- **Success Response (200 OK)**:
  ```json
  {
    "status": "SUCCESS",
    "message": "Conflicts evaluated",
    "data": [
      {
        "conflictType": "OVER_ALLOCATION_CONFLICT",
        "severity": "CRITICAL",
        "developerId": 2,
        "developerName": "Priya Sharma",
        "description": "Priya Sharma allocated to 110% peak load (exceeds 100% maximum capacity).",
        "recommendation": "Reduce allocation on Milestone 5 or swap with Developer Vikram."
      }
    ]
  }
  ```

### `GET /api/skill-gaps`
- **Purpose**: List all milestones where the assigned developer lacks required skills.

---

## 9. Budget & Forecasting

### `GET /api/projects/{id}/budget`
- **Purpose**: Calculate daily burn rate and forecast exhaustion date.
- **Success Response (200 OK)**:
  ```json
  {
    "status": "SUCCESS",
    "message": "Budget metrics computed",
    "data": {
      "projectId": 1,
      "projectName": "Project Alpha",
      "totalBudget": 500000.00,
      "currentSpend": 350000.00,
      "remainingBudget": 150000.00,
      "elapsedDays": 20,
      "dailyBurnRate": 17500.00,
      "forecastExhaustionDate": "2026-10-29",
      "budgetStatus": "CRITICAL"
    }
  }
  ```

---

## 10. What-If Simulator

### `POST /api/simulations`
- **Purpose**: Simulate swapping candidate developer on milestone without modifying database.
- **Request Body**:
  ```json
  {
    "milestoneId": 1,
    "currentDeveloperId": 2,
    "proposedDeveloperId": 5,
    "allocationPercentage": 60
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "status": "SUCCESS",
    "message": "Simulation computed",
    "data": {
      "simulationId": 101,
      "costDifference": -16000.00,
      "deliveryDaysDelta": 0,
      "scenarioVerdict": "OPTIMAL",
      "recommendation": "Swap saves ₹16,000 with 100% skill match."
    }
  }
  ```

### `POST /api/simulations/{id}/apply`
- **Purpose**: Explicitly commit the simulated swap to the real database.

---

## 11. Dashboards & Notifications

### `GET /api/dashboard/manager`
- **Purpose**: Summary metrics for Project Manager.

### `GET /api/dashboard/developer/{id}`
- **Purpose**: Personal assignments, workload, and notifications for Developer.

### `GET /api/notifications`
- **Purpose**: Retrieve system notifications.

### `PUT /api/notifications/{id}/read`
- **Purpose**: Mark notification as read.
