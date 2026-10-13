# Verify the current phase

Run this at the end of every phase, before reporting it as done.

1. Re-read the phase in `BUILD_PROMPT.md` section 18 and list its "Done when" checks.
2. Backend: in `backend/`, run `mvn -q verify`. Paste the final result lines. If anything fails, fix the root cause and run again.
3. List every file created or changed in this phase. For each, confirm it matches the specification section it implements (name the section).
4. Search the changed code for: `TODO`, `FIXME`, `double` used for money, `javax.`, `WebSecurityConfigurerAdapter`, hard-coded dates, hard-coded dashboard numbers, database calls inside loops. Report each hit and fix it.
5. Run each "Done when" check (Postman request, curl command, SQL query, browser page, or test) and paste the evidence.
6. If the phase touches calculations, run the matching worked-example tests from `BUILD_PROMPT.md` section 16 and show the expected vs actual values.
7. Update `docs/PROGRESS.md` (phase, status, date, evidence) and `docs/DECISIONS.md` (any new decisions).
8. Write the phase report: done, evidence, decisions, open items. Then stop and wait for the user.
