# Architecture

**Project Resource Allocation System — SIST CSE Capstone Project**

---

## 1. Technology stack

| Layer | Technology | Why |
|---|---|---|
| Presentation | HTML5, CSS3, vanilla JavaScript | The stack the project is required to use. No framework, no build step — open a file and read it. |
| Application | Java 21 + Spring Boot 3.3.5 | Spring Web gives the REST controllers; Spring Data JPA maps Java classes to MySQL tables. |
| Data | MySQL 8.0 | Required by the specification. |
| Build | Maven | Downloads the libraries and builds one runnable `.jar`. |
| Testing | JUnit 5, Mockito, Postman | 39 unit tests for the business rules; a Postman collection of 85 requests for the API. |
| PDF export | OpenPDF | The only library outside the named stack — see section 7. |

---

## 2. The big picture

```
        BROWSER
┌──────────────────────────────────┐
│  index.html   dashboard.html     │
│  projects.html  developers.html  │
│  allocations.html conflicts.html │
│  budget.html  timeline.html      │
│  simulator.html                  │
│  developer-dashboard.html        │
│                                  │
│  css/style.css                   │
│  js/api.js + one file per page   │
└───────────────┬──────────────────┘
                │  fetch()  —  JSON over HTTP
                ▼
        SPRING BOOT  (port 8080)
┌──────────────────────────────────┐
│  controller/   @RestController   │   receives the request, validates the
│        │                         │   DTO, returns a DTO. No logic here.
│        ▼                         │
│  service/      @Service          │   ALL the business rules live here.
│        │                         │
│        ▼                         │
│  repository/   JpaRepository     │   Spring writes the SQL from the
│                                  │   method names.
│  entity/ dto/ exception/ config/ │
└───────────────┬──────────────────┘
                │  JDBC
                ▼
        MySQL 8.0  —  resource_allocation_db
        13 tables (see database-design.md)
```

---

## 3. Why the layers exist

**Controller** — knows only about HTTP. It reads the request body into a DTO,
lets Bean Validation check the simple rules (`@NotBlank`, `@Email`, `@Min`),
calls one service method and returns the result. If a controller ever grew an
`if` statement about allocation percentages, that logic would belong in a service.

**Service** — the actual project. `ConflictService` sweeps a developer's
timeline, `SkillGapService` compares required against covered skills,
`BudgetService` does the money arithmetic, `SimulationService` answers the
what-if question without writing anything. These classes take plain Java
objects and return plain Java objects, which is exactly why most of them can
be unit tested with no database at all.

**Repository** — a Spring Data JPA interface. Writing

```java
List<ResourceAllocation> findByDeveloperId(Long developerId);
```

is enough; Spring generates `SELECT * FROM resource_allocations WHERE developer_id = ?`
at start-up. No SQL is written by hand anywhere in the project.

**Entity** — one Java class per table, mapped with JPA annotations
(`@Entity`, `@Table`, `@Column`, `@ManyToOne`).

**DTO** — the shape sent to and received from the browser. Keeping DTOs
separate from entities means the API never accidentally exposes a database
column, and the JSON can carry extra fields (such as an allocation's
`warnings` list) that no table has.

**exception/** — `ResourceNotFoundException` becomes HTTP 404 and
`ValidationException` becomes HTTP 400. A single `@RestControllerAdvice`
turns both into the same JSON shape, so the browser never sees a Java stack
trace.

---

## 4. Request walk-through

What happens when the manager presses **Save allocation**:

1. `allocations.js` calls `api.post('/api/allocations', payload)`.
2. `AllocationController.create()` receives it. `@Valid` checks the simple
   field rules; a bad percentage is rejected here with HTTP 400.
3. `AllocationService.create()` loads the developer, project and milestone.
   Unknown ids raise `ResourceNotFoundException` → 404. Dates outside the
   project raise `ValidationException` → 400.
4. The row is saved.
5. `SkillGapService` is asked whether the developer is missing any skill the
   milestone requires. If so, a warning is collected and a SKILL_GAP
   notification is stored — the assignment is never silent.
6. `ConflictService` re-runs over that developer. Any conflict touching the
   new allocation is added to the warnings and stored as a notification.
7. The controller returns HTTP 201 with the allocation **and** the warnings.
8. `allocations.js` shows the warnings in an amber box, or a green
   "no conflict and no skill gap" box when the list is empty.

---

## 5. How the frontend is organised

* `js/api.js` is loaded by every page. It holds the `fetch()` wrapper, the
  date and rupee formatters, `escapeHtml()`, the navigation bar and the
  shared drawing helpers (`barHtml`, `statusBadge`, `skillChips`).
* Each page has exactly one matching script: `dashboard.html` →
  `js/dashboard.js`. That script waits for `DOMContentLoaded`, calls the API
  and writes HTML into the containers.
* The two dashboards each call a **single** endpoint
  (`/api/dashboard/summary` and `/api/dashboard/developer/{id}`) so one page
  load is one request.
* There are no charting libraries. A percentage bar is a `<div>` inside a
  `<div>` with `style="width:60%"`. The timeline positions each milestone bar
  with `left` and `width` percentages calculated from the project's dates.
* Conflicts are made obvious with CSS: `tr.row-conflict` gets a red
  background and a thick red left border.

---

## 6. Security note (worth saying out loud in the viva)

The login is deliberately minimal — it only decides which dashboard opens.
Passwords are stored as plain text in the `users` table and there is no
Spring Security, no session token and no server-side authorisation check on
the API. A production system would store a BCrypt hash and protect every
endpoint. That was left out on purpose to keep the project inside the agreed
technology stack and small enough to explain. It is a known limitation, not
an oversight.

---

## 7. The one extra dependency: OpenPDF

**Why it is needed.** The specification asks for a PDF export of the what-if
report. Java has no PDF writer in its standard library, so a library is
unavoidable.

**What it does.** OpenPDF builds a PDF from Java code:

```java
Document document = new Document(PageSize.A4);
PdfWriter.getInstance(document, outputStream);
document.open();
document.add(new Paragraph("What-If Resource Swap Report"));
document.add(table);
document.close();
```

`ReportService` uses about 60 lines of it.

**Can the project work without it?** Yes. The simulator page also has a
**Print / Save as PDF** button that uses the browser's own print dialog,
with a print stylesheet (`@media print`) that hides the navigation and
buttons. OpenPDF only adds a proper server-side download at
`GET /api/simulations/{id}/report`.

**Why OpenPDF and not Apache PDFBox?** PDFBox is a low-level library where
text is placed at x/y coordinates. OpenPDF has a document-and-paragraph API
that reads like ordinary Java, so it is far easier to explain.

---

## 8. Files to show during a demo

| Question the examiner may ask | File to open |
|---|---|
| "How do you detect over-allocation?" | `service/ConflictService.java` |
| "How do you avoid dividing by zero?" | `service/BudgetService.java` |
| "How is the delivery date calculated?" | `service/DeliveryEstimateService.java` |
| "How do you find skill gaps?" | `service/SkillGapService.java` |
| "How does the simulator avoid changing data?" | `service/SimulationService.java` (`run()` never calls `save()`) |
| "How does the browser talk to the server?" | `frontend/js/api.js` |
| "Where are the tables defined?" | `database/schema.sql` |
| "How do you know it works?" | `backend/src/test/java/.../ConflictServiceTest.java` |
