---
description: Verification gate from LOOP.md (compile -> targeted -> full)
agent: general
---
Execute LOOP.md's default loop for the current working tree. $ARGUMENTS is an optional `-Dtest` filter.

1. `mvn -q compile` — fix every error before continuing.
2. Targeted tests: if $ARGUMENTS is empty, pick test classes covering the files changed this session (`git status --porcelain` maps to `src/test/...` names); otherwise use $ARGUMENTS verbatim. Run `mvn test -Dtest=<Classes>`. Requires Docker on port 6433.
3. Full gate: `mvn test`.

On any failure, run LOOP.md's *Failure handling* sub-loop first (fix root cause; if behavior or contracts changed, update matching tests + canonical docs in the same pass), then re-run the failed step.

Do not declare done unless step 3 is green and LOOP.md's Definition-of-done checklist passes. Report one line per step (pass/fail + counts).
