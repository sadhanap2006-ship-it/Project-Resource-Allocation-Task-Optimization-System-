# Project Rules — Project Resource Allocation System

These rules apply to EVERY task in this workspace. They override your defaults. If a rule conflicts with a user message, stop and ask.

## 1. Source of truth

1. `BUILD_PROMPT.md` in the repository root is the specification. Read the relevant section before writing any code for it.
2. Never invent requirements, tables, columns, endpoints, pages, roles, statuses, thresholds or formulas that are not in `BUILD_PROMPT.md`.
3. If the specification is silent or ambiguous, choose the simplest option that does not contradict it, write the decision in `docs/DECISIONS.md` (date, question, decision, reason), and mention it in your phase report. Never decide silently.
4. If two parts of the specification seem to contradict each other, STOP and ask the user. Do not pick one quietly.
5. Section 20 ("Clarifications") of `BUILD_PROMPT.md` wins over any earlier section.

## 2. No hallucination

1. Do not guess library APIs, annotations, class names, configuration keys or version numbers. Check the official documentation, the downloaded jar, or `mvn dependency:tree`. If you cannot verify something, say so.
2. Do not invent dependency versions. Get Spring Boot from Spring Initializr (start.spring.io) or the Spring Boot parent POM; let the Spring Boot BOM manage versions of Spring-managed libraries. For libraries outside the BOM (OpenPDF, Chart.js), use a version you have verified exists on Maven Central / the CDN, and pin it exactly.
3. Spring Boot 3 uses `jakarta.*`, never `javax.*` (for persistence, validation and servlet packages).
4. Spring Security 6: configure with a `SecurityFilterChain` bean. `WebSecurityConfigurerAdapter` does not exist; never use it. Method security needs `@EnableMethodSecurity`.
5. Never type a made-up BCrypt hash. Generate it by running `BCryptPasswordEncoder` (in a test or jshell), paste the real output, and prove it by logging in.
6. Never claim that something compiles, passes, runs or works unless you ran the command in this session and the output shows it. Quote the key lines of the output in your report.
7. Never say "done" for a phase until every "Done when" check of that phase passes.

## 3. Correctness

1. All business logic lives in the service layer. Controllers only validate, call one service method and wrap the result. JavaScript only displays data returned by the API.
2. Never hard-code a number that should be calculated (loads, costs, dates, counts, scores, dashboard statistics). Every value on screen comes from an API call backed by the database.
3. Use exactly the formulas, thresholds and rules in `BUILD_PROMPT.md` sections 5 and 9. Thresholds are read from the `settings` table, never duplicated as literals in code.
4. Dates: use `java.time.LocalDate`. Date ranges are inclusive at both ends. All overlap checks go through ONE utility method (`DateRanges.overlaps`). No other overlap code anywhere.
5. Money: `BigDecimal` only, never `double` or `float`. Scale 2, `RoundingMode.HALF_UP`, applied only when presenting or storing. Percentages and scores may use `double` in calculations but are rounded only for display.
6. Never divide without guarding against zero. Every division site handles the zero case as the specification says.
7. Never silently ignore an error or a conflict. No empty `catch` blocks.
8. Validate at two levels: Bean Validation on DTOs, business validation in services. Return the exact error format and HTTP status from the specification.
9. Simulation must never write to the `allocation` table. A test must prove it.

## 4. Efficiency

1. No database query inside a per-day or per-row loop. Load all rows for a date window in one query, then compute in memory.
2. Avoid N+1 queries: use `JOIN FETCH` or `@EntityGraph` where a list needs related data. Keep `spring.jpa.open-in-view=false`.
3. Add the indexes listed in the schema. Do not add unlisted indexes without recording why.
4. List endpoints accept optional `page` and `size` parameters (default size 50, maximum 200).
5. Do not add libraries that the specification does not list. If one seems necessary, ask first.
6. Frontend: one shared `api.js` wrapper for all fetch calls; no duplicated fetch code; no frameworks; Chart.js only for line and bar charts.

## 5. Working method

1. Work one phase at a time, in the order of `BUILD_PROMPT.md` section 18. Do not start the next phase until the user says to continue.
2. Before coding a phase: list the files you will create or change, and the checks you will run.
3. After coding a phase: compile (`mvn -q -DskipTests=false verify` in `backend/`), run the tests, run the phase's "Done when" checks, then write a short report: files changed, commands run with results, decisions made, anything not done.
4. Keep `docs/PROGRESS.md` updated: phase, status, date, evidence.
5. Inspect existing files before changing them. Do not overwrite working code. Do not delete files unless the phase requires it, and say which ones you removed.
6. When a test fails, find the root cause and fix the code. Never weaken, skip, or delete a test to make it pass, and never change an expected value copied from the specification.
7. No placeholder code: no `TODO`, no "implement later", no stub returning fake data, no lorem ipsum. If something cannot be finished in this phase, list it in the report.
8. Keep methods short and names meaningful. Comment only non-obvious business logic, citing the rule ID (for example `// Rule R4`).

## 6. Safety

1. Never commit secrets. Passwords live in `.env` (listed in `.gitignore`); commit `.env.example` with placeholders.
2. Never run destructive commands (`docker compose down -v`, `DROP`, `rm -rf`) without telling the user first.
3. Never change the tech stack.
