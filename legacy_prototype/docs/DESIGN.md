# DESIGN PHASE: Project Resource Allocation System

## SIST CSE AI Capstone Project Technical Design Document

---

## 1. System Architecture Overview

The system is built as a modular, 3-tier web application using standard technologies designed for high explainability, zero dependency bloat, and straightforward local deployment.

```text
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT / FRONTEND                        │
│   Vanilla HTML5 + CSS3 (Grid/Flexbox) + ES6 JavaScript      │
│   Chart.js for Visualizations | Browser Print/PDF Workflow  │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON (REST API)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   SERVER / APPLICATION TIER                 │
│         Java SE com.sun.net.httpserver.HttpServer           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Controller Layer: Routes, Request Parsing, Statuses   │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ Service Layer: Conflict Engine, What-If Sim, Budget   │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ DAO Layer: JDBC PreparedStatements, ResultSets        │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC (MySQL Connector/J)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     DATABASE TIER (MySQL 8.0)               │
│        Normalized 3NF Relational Tables with Constraints     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Component Responsibilities

### 2.1 Controller Layer (`backend/src/controller/`)
- Intercepts HTTP requests dispatched by `com.sun.net.httpserver.HttpServer`.
- Extracts URL query parameters, path variables, and JSON request bodies.
- Enforces authentication context where required.
- Maps business outcomes to standard HTTP response codes (`200 OK`, `201 CREATED`, `400 BAD REQUEST`, `401 UNAUTHORIZED`, `404 NOT FOUND`, `409 CONFLICT`, `500 INTERNAL SERVER ERROR`).
- Emits structured JSON payloads formatted as `{ "status": "SUCCESS|ERROR", "message": "...", "data": ... }`.

### 2.2 Service Layer (`backend/src/service/`)
- **`AuthService`**: Authenticates user credentials and resolves role permissions.
- **`ConflictDetectionService`**: Implements interval-based sweep-line conflict detection. For each developer, identifies overlapping milestone date intervals, computes peak cumulative allocation, and flags over-allocations ($>100\%$) and leave overlaps.
- **`SkillGapService`**: Cross-references milestone required skills against developer skill junctions.
- **`BudgetService`**: Calculates elapsed project days, average daily burn rate, and projected exhaustion dates.
- **`SimulationService`**: Evaluates in-memory candidate swaps without mutating MySQL, recalculating projected cost differences, delivery dates, and skill fits.
- **`NotificationService`**: Persists system alerts and allows reading/dismissal.

### 2.3 DAO Layer (`backend/src/dao/`)
- Encapsulates all direct SQL execution via standard JDBC `PreparedStatement`.
- Eliminates SQL injection vulnerabilities through parameterized values.
- Uses try-with-resources blocks for automatic closing of `Connection`, `PreparedStatement`, and `ResultSet`.

### 2.4 Utility Layer (`backend/src/util/`)
- **`DBConnection`**: Manages MySQL connection establishment using standard JDBC URL, user, and password parameters.
- **`JsonUtil`**: Lightweight, pure-Java JSON serializer and parser avoiding external dependency bloat.
- **`ValidationUtil`**: Enforces business rules (dates, percentages, email formats, positive amounts).

---

## 3. Request-Response Flow

```text
User Action in Browser
   ↓
JavaScript `fetch('/api/allocations', { method: 'POST', body: ... })`
   ↓
Java HttpServer Context Handler (`/api/allocations`)
   ↓
`AllocationController.handlePost(exchange)`
   ↓
`ValidationUtil.validateAllocationRequest(...)`
   ↓
`AllocationService.createAllocation(...)`
   ↓
`ConflictDetectionService.checkDeveloperOverAllocation(...)`
   ↓
`AllocationDAO.insertAllocation(...)` (via JDBC PreparedStatement)
   ↓
MySQL COMMIT & Generated Key Return
   ↓
Controller packages response via `JsonUtil`
   ↓
HttpServer sends HTTP 201 Created with JSON payload
   ↓
Frontend JavaScript receives response, updates UI, and displays toast notification
```

---

## 4. Security & Error Handling Principles

1. **SQL Injection Prevention**: Every query uses `?` placeholders with `PreparedStatement`. Never string concatenation.
2. **Input Sanitization**: Numbers, dates, and percentage limits are validated on both client and server tiers.
3. **No Stack Traces to Client**: Exceptions caught at controller boundaries return clean, user-friendly JSON error messages with appropriate HTTP status codes.
4. **Role Isolation**: Developer accounts cannot trigger PM administrative endpoints (e.g. project creation, budget modification, or swap application).
