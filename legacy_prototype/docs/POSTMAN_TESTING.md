# Postman & cURL API Testing Guide

## SIST CSE AI Capstone Project — Project Resource Allocation System

This guide provides ready-to-use **cURL commands** and request payloads to test all REST endpoints of the pure Java backend (`http://localhost:8080`) using Postman or Windows PowerShell.

---

## 1. Authentication Endpoints

### 1.1 Login as Project Manager
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"manager@company.com\",\"password\":\"admin123\"}"
```
**Expected Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Authentication successful",
  "data": {
    "userId": 1,
    "name": "Rajesh Ramanathan",
    "email": "manager@company.com",
    "role": "PROJECT_MANAGER",
    "developerId": null
  }
}
```

### 1.2 Login as Developer
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"arun@company.com\",\"password\":\"dev123\"}"
```
**Expected Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Authentication successful",
  "data": {
    "userId": 2,
    "name": "Arun Kumar",
    "email": "arun@company.com",
    "role": "DEVELOPER",
    "developerId": 1
  }
}
```

---

## 2. Dashboard Endpoints

### 2.1 Get Manager Dashboard Metrics
```bash
curl -X GET http://localhost:8080/api/dashboard/manager
```
**Expected Response (200 OK):** Returns summary cards (active projects, utilization %, budget spend, critical conflict count) and list of project summaries.

### 2.2 Get Developer Portal Profile
```bash
curl -X GET http://localhost:8080/api/dashboard/developer/1
```
**Expected Response (200 OK):** Returns Arun Kumar's current load (70%), remaining capacity (30%), assigned milestones, and system notifications.

---

## 3. Conflict Detection & Skill Gap Endpoints

### 3.1 Get All Evaluated Conflicts
```bash
curl -X GET http://localhost:8080/api/conflicts
```
**Expected Response (200 OK):** Evaluates over-allocations (>100%), leave intersections, and skill gaps in real time.

### 3.2 Get Milestone Skill Gaps
```bash
curl -X GET http://localhost:8080/api/skill-gaps
```
**Expected Response (200 OK):** Returns list of milestones with missing competencies and required training/reassignment recommendations.

---

## 4. Project & Milestone Management

### 4.1 List All Projects
```bash
curl -X GET http://localhost:8080/api/projects
```

### 4.2 Create New Project
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Project Epsilon — Micro-Frontend Platform\",\"description\":\"Decoupled UI architecture with web components.\",\"startDate\":\"2026-11-15\",\"endDate\":\"2027-03-31\",\"totalBudget\":550000.00}"
```

### 4.3 List Milestones for Project 1
```bash
curl -X GET "http://localhost:8080/api/milestones?projectId=1"
```

---

## 5. Resource Directory & Allocation

### 5.1 List All Developers
```bash
curl -X GET http://localhost:8080/api/developers
```

### 5.2 Create New Resource Allocation
```bash
curl -X POST http://localhost:8080/api/allocations \
  -H "Content-Type: application/json" \
  -d "{\"developerId\":4,\"projectId\":2,\"milestoneId\":6,\"allocationPercentage\":50,\"startDate\":\"2026-11-01\",\"endDate\":\"2026-12-20\",\"estimatedHours\":80}"
```

### 5.3 Delete Resource Allocation
```bash
curl -X DELETE "http://localhost:8080/api/allocations?id=6"
```

---

## 6. What-If Scenario Simulation

### 6.1 Run In-Memory What-If Simulation (Zero DB Mutation)
```bash
curl -X POST http://localhost:8080/api/simulations \
  -H "Content-Type: application/json" \
  -d "{\"milestoneId\":1,\"currentDeveloperId\":2,\"proposedDeveloperId\":1,\"allocationPercentage\":60}"
```
**Expected Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "What-if simulation evaluated (Database NOT modified)",
  "data": {
    "milestoneId": 1,
    "milestoneName": "Core Auth & Security Service",
    "currentDeveloperName": "Priya Sharma",
    "proposedDeveloperName": "Arun Kumar",
    "currentCostPerHour": 800.0,
    "proposedCostPerHour": 600.0,
    "milestoneCostDifference": -16000.0,
    "scenarioVerdict": "CRITICAL_RISK",
    "recommendation": "Reject swap: Arun Kumar would exceed 100% capacity (130%)."
  }
}
```

### 6.2 Apply Approved What-If Scenario to Database
```bash
curl -X POST http://localhost:8080/api/simulations/1001/apply
```

---

## 7. Importing into Postman GUI
1. Open Postman.
2. Click **Import** > **Raw text**.
3. Paste any of the `curl` commands above.
4. Click **Run** and view live JSON responses directly from your local Java server!
