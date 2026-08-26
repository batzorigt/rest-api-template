---
name: repo-loops
description: Verification loops for this repo (compile -> targeted tests -> full gate, per-change-type loops, failure handling, definition of done). Load before running checks or declaring work done.
type: Playbook
title: Repo verification loops quick playbook
resource: /LOOP.md
tags: [loops, verification]
status: stable
stale_after: 2027-02-26T00:00:00Z
---
Distilled from `LOOP.md` (canonical — read it fully before verifying non-trivial changes).

## When to load

Before running any verification checks or declaring work done.

## Preconditions

- Docker daemon running (see the harness skill for port/details).
- Change already compiled at least once mentally mapped to its loop: entity, DTO, endpoint, authorization, template, or deployment.

## Steps

1. `mvn -q compile` — fix every error before continuing.
2. Targeted tests: `mvn test -Dtest=Class[#method]`; pick classes covering the files changed this session.
3. Full gate: `mvn test`. Never declare done without step 3 green.

On any failure: fix the smallest root cause; if behavior or contracts changed, resync matching tests + canonical docs in the same pass; re-run the failed step first (LOOP.md → Failure handling).

## Post-conditions

LOOP.md's Definition-of-done checklist passes — tests, doc sync, neutrality, anti-duplication, generated files untouched.
