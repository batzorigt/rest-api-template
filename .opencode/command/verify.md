---
description: Verification gate from LOOP.md (compile -> targeted -> full)
agent: general
---
Execute the repo's verification gate. $ARGUMENTS is an optional `-Dtest` filter.

Load the `repo-loops` skill (or read `LOOP.md` directly) and run its default loop exactly, in order. This adapter adds only:

- Test selection: if $ARGUMENTS is empty, pick test classes covering the files changed this session (`git status --porcelain` maps to `src/test/...` names); otherwise use $ARGUMENTS verbatim.
- Failure path: on any failing step, apply LOOP.md's Failure-handling sub-loop before re-running it.

Do not declare done unless the full gate is green and the Definition-of-done checklist passes. Report one line per step (pass/fail + counts).
